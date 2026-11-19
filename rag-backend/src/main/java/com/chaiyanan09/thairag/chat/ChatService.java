package com.chaiyanan09.thairag.chat;

import com.chaiyanan09.thairag.chunk.ChunkRepository;
import com.chaiyanan09.thairag.chunk.SearchHit;
import com.chaiyanan09.thairag.client.ModelServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RAG: ค้น chunk ที่เกี่ยวข้อง -> ส่งให้ LLM พร้อมกติกา -> ได้คำตอบที่มีเลขอ้างอิง
 *
 * ขั้นตอน
 * 1. แปลงคำถามเป็น vector (Python /embed) แล้วค้น top-k chunk ด้วย pgvector
 *    (ถ้าแชทมีไฟล์แนบ ค้นเฉพาะไฟล์เหล่านั้น ถ้าไม่มีค้นทุกเอกสาร)
 * 2. ตัด chunk ที่คะแนนต่ำกว่า min-score ทิ้ง ถ้าไม่เหลือเลยตอบ "ไม่พบ" ทันทีโดยไม่เรียก LLM
 * 3. ส่ง chunk พร้อมรหัส [S1]..[Sk] ให้ Typhoon ผ่าน Spring AI ChatClient
 * 4. อ่านรหัสอ้างอิงจากคำตอบ จัดให้อยู่รูปแบบ [S1] แล้วบอกหน้าเว็บว่าชิ้นไหนถูกอ้างถึง
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final int MAX_QUESTION_LENGTH = 1000;
    private static final int MAX_DOCUMENTS = 20;
    static final String NO_DOCUMENTS = "ยังไม่มีเอกสารในระบบ กดปุ่ม + เพื่อแนบไฟล์ PDF ก่อน";
    static final String NO_ATTACHED_DOCUMENTS = "ไม่พบไฟล์ที่แนบในระบบ (อาจถูกลบไปแล้ว) ลองแนบไฟล์ใหม่หรือเริ่มแชทใหม่";

    private final ChatClient chatClient;
    private final ChunkRepository chunks;
    private final ModelServiceClient modelService;
    private final ChatLogRepository chatLogs;
    private final String model;
    private final int topK;
    private final double minScore;
    private final PromptBuilder.SourceFormat sourceFormat;
    private final boolean verifyCitations;

    public ChatService(ChatClient.Builder chatClientBuilder,
                       ChunkRepository chunks,
                       ModelServiceClient modelService,
                       ChatLogRepository chatLogs,
                       @Value("${spring.ai.openai.chat.model:unknown}") String model,
                       @Value("${app.chat.top-k:5}") int topK,
                       @Value("${app.chat.min-score:0.35}") double minScore,
                       @Value("${app.chat.source-format:plain}") String sourceFormat,
                       @Value("${app.chat.verify-citations:true}") boolean verifyCitations) {
        this.chatClient = chatClientBuilder.defaultSystem(PromptBuilder.SYSTEM_PROMPT).build();
        this.chunks = chunks;
        this.modelService = modelService;
        this.chatLogs = chatLogs;
        this.model = model;
        this.topK = topK;
        this.minScore = minScore;
        this.sourceFormat = PromptBuilder.SourceFormat.valueOf(sourceFormat.strip().toUpperCase());
        this.verifyCitations = verifyCitations;
        log.info("Chat settings: {}", settings());
    }

    /** ค่าที่ใช้อยู่ตอนนี้ ให้ eval บันทึกไว้คู่กับผลลัพธ์ */
    public ChatSettings settings() {
        return new ChatSettings(model, topK, minScore, sourceFormat.name().toLowerCase(),
                PromptBuilder.PROMPT_VERSION, verifyCitations);
    }

    /** ค้นจากทุกเอกสารในระบบ (eval ใช้แบบนี้) */
    public ChatReply ask(String rawQuestion) {
        return ask(rawQuestion, List.of());
    }

    /** ถ้ามี documentIds จะค้นเฉพาะในไฟล์เหล่านั้น เหมือนแนบไฟล์ให้ AI อ่านเฉพาะในแชทนี้ */
    public ChatReply ask(String rawQuestion, List<UUID> documentIds) {
        Set<UUID> scope = documentIds == null ? Set.of() : documentIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (scope.size() > MAX_DOCUMENTS) {
            throw new IllegalArgumentException("แนบไฟล์ได้ไม่เกิน " + MAX_DOCUMENTS + " ไฟล์ต่อแชท");
        }
        String question = rawQuestion == null ? "" : rawQuestion.strip();
        if (question.isEmpty()) {
            throw new IllegalArgumentException("กรุณาพิมพ์คำถาม");
        }
        if (question.length() > MAX_QUESTION_LENGTH) {
            throw new IllegalArgumentException("คำถามยาวเกิน " + MAX_QUESTION_LENGTH + " ตัวอักษร");
        }

        long start = System.nanoTime();
        float[] queryVector = modelService.embed(List.of(question)).get(0);
        List<SearchHit> hits = chunks.search(queryVector, topK, scope);
        List<SearchHit> relevant = hits.stream().filter(h -> h.score() >= minScore).toList();
        long retrievalMs = millisSince(start);

        if (relevant.isEmpty()) {
            String answer = !hits.isEmpty() ? PromptBuilder.NOT_FOUND
                    : scope.isEmpty() ? NO_DOCUMENTS : NO_ATTACHED_DOCUMENTS;
            return save(new ChatReply(question, answer, false, List.of(), model, retrievalMs, 0, 0));
        }

        long llmStart = System.nanoTime();
        String answer = callLlm(PromptBuilder.userPrompt(question, relevant, sourceFormat));
        long llmMs = millisSince(llmStart);

        String rawAnswer = answer;
        CitationParser.Result citations = CitationParser.normalize(answer, relevant.size());
        boolean found = PromptBuilder.isFound(answer, citations.cited());
        Set<Integer> cited = Set.of();
        int citationFixes = 0;
        if (found) {
            answer = citations.text();
            cited = citations.cited();
            if (verifyCitations) {
                // เทียบข้อความจริงว่าแต่ละประโยคมาจากชิ้นไหน แล้วแก้เลขอ้างอิงที่ผิดหรือขาด
                CitationVerifier.Result verified = CitationVerifier.verify(
                        answer, relevant.stream().map(SearchHit::content).toList());
                answer = verified.text();
                cited = verified.cited();
                citationFixes = verified.changes();
            }
        } else {
            answer = PromptBuilder.NOT_FOUND;
        }

        List<ChatReply.Source> sources = new ArrayList<>(relevant.size());
        for (int i = 0; i < relevant.size(); i++) {
            SearchHit hit = relevant.get(i);
            sources.add(new ChatReply.Source(i + 1, hit.chunkId(), hit.documentId(), hit.filename(),
                    hit.page(), hit.content(), hit.score(), cited.contains(i + 1)));
        }
        log.info("chat: scope {}, {} chunks, cited {}, found={}, fixes={}, retrieval {} ms, llm {} ms, answer: {}",
                scope.isEmpty() ? "all" : scope.size() + " files", relevant.size(), cited, found, citationFixes,
                retrievalMs, llmMs, abbreviate(rawAnswer));
        return save(new ChatReply(question, answer, found, sources, model, retrievalMs, llmMs, citationFixes));
    }

    private String callLlm(String userPrompt) {
        try {
            return CitationParser.clean(chatClient.prompt().user(userPrompt).call().content());
        } catch (RuntimeException e) {
            String detail = String.valueOf(e.getMessage());
            String hint = detail.contains("401") || detail.toLowerCase().contains("unauthorized")
                    ? " (ตรวจ TYPHOON_API_KEY ใน rag-backend/.env)"
                    : "";
            throw new LlmException("เรียก LLM ไม่สำเร็จ" + hint + ": " + detail, e);
        }
    }

    /** บันทึก log ถ้าบันทึกไม่ได้ก็ยังส่งคำตอบให้ผู้ใช้ตามปกติ */
    private ChatReply save(ChatReply reply) {
        try {
            chatLogs.insert(reply);
        } catch (RuntimeException e) {
            log.warn("Could not save chat log: {}", e.getMessage());
        }
        return reply;
    }

    private static String abbreviate(String text) {
        String oneLine = text.replace('\n', ' ');
        return oneLine.length() <= 120 ? oneLine : oneLine.substring(0, 120) + "...";
    }

    private static long millisSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
