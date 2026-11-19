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
import java.util.List;
import java.util.Set;

/**
 * RAG: ค้น chunk ที่เกี่ยวข้อง -> ส่งให้ LLM พร้อมกติกา -> ได้คำตอบที่มีเลขอ้างอิง
 *
 * ขั้นตอน
 * 1. แปลงคำถามเป็น vector (Python /embed) แล้วค้น top-k chunk ด้วย pgvector
 * 2. ตัด chunk ที่คะแนนต่ำกว่า min-score ทิ้ง ถ้าไม่เหลือเลยตอบ "ไม่พบ" ทันทีโดยไม่เรียก LLM
 * 3. ส่ง chunk พร้อมรหัส [S1]..[Sk] ให้ Typhoon ผ่าน Spring AI ChatClient
 * 4. อ่านรหัสอ้างอิงจากคำตอบ จัดให้อยู่รูปแบบ [S1] แล้วบอกหน้าเว็บว่าชิ้นไหนถูกอ้างถึง
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final int MAX_QUESTION_LENGTH = 1000;
    static final String NO_DOCUMENTS = "ยังไม่มีเอกสารในระบบ กรุณาอัปโหลดเอกสารก่อน";

    private final ChatClient chatClient;
    private final ChunkRepository chunks;
    private final ModelServiceClient modelService;
    private final ChatLogRepository chatLogs;
    private final String model;
    private final int topK;
    private final double minScore;

    public ChatService(ChatClient.Builder chatClientBuilder,
                       ChunkRepository chunks,
                       ModelServiceClient modelService,
                       ChatLogRepository chatLogs,
                       @Value("${spring.ai.openai.chat.model:unknown}") String model,
                       @Value("${app.chat.top-k:5}") int topK,
                       @Value("${app.chat.min-score:0.35}") double minScore) {
        this.chatClient = chatClientBuilder.defaultSystem(PromptBuilder.SYSTEM_PROMPT).build();
        this.chunks = chunks;
        this.modelService = modelService;
        this.chatLogs = chatLogs;
        this.model = model;
        this.topK = topK;
        this.minScore = minScore;
    }

    public ChatReply ask(String rawQuestion) {
        String question = rawQuestion == null ? "" : rawQuestion.strip();
        if (question.isEmpty()) {
            throw new IllegalArgumentException("กรุณาพิมพ์คำถาม");
        }
        if (question.length() > MAX_QUESTION_LENGTH) {
            throw new IllegalArgumentException("คำถามยาวเกิน " + MAX_QUESTION_LENGTH + " ตัวอักษร");
        }

        long start = System.nanoTime();
        float[] queryVector = modelService.embed(List.of(question)).get(0);
        List<SearchHit> hits = chunks.search(queryVector, topK);
        List<SearchHit> relevant = hits.stream().filter(h -> h.score() >= minScore).toList();
        long retrievalMs = millisSince(start);

        if (relevant.isEmpty()) {
            String answer = hits.isEmpty() ? NO_DOCUMENTS : PromptBuilder.NOT_FOUND;
            return save(new ChatReply(question, answer, false, List.of(), model, retrievalMs, 0));
        }

        long llmStart = System.nanoTime();
        String answer = callLlm(PromptBuilder.userPrompt(question, relevant));
        long llmMs = millisSince(llmStart);

        String rawAnswer = answer;
        CitationParser.Result citations = CitationParser.normalize(answer, relevant.size());
        boolean found = PromptBuilder.isFound(answer, citations.cited());
        Set<Integer> cited = found ? citations.cited() : Set.of();
        answer = found ? citations.text() : PromptBuilder.NOT_FOUND;

        List<ChatReply.Source> sources = new ArrayList<>(relevant.size());
        for (int i = 0; i < relevant.size(); i++) {
            SearchHit hit = relevant.get(i);
            sources.add(new ChatReply.Source(i + 1, hit.chunkId(), hit.documentId(), hit.filename(),
                    hit.page(), hit.content(), hit.score(), cited.contains(i + 1)));
        }
        log.info("chat: {} chunks, cited {}, found={}, retrieval {} ms, llm {} ms, answer: {}",
                relevant.size(), cited, found, retrievalMs, llmMs, abbreviate(rawAnswer));
        return save(new ChatReply(question, answer, found, sources, model, retrievalMs, llmMs));
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
