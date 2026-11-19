package com.chaiyanan09.thairag.chat;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** บันทึกทุกคำถาม-คำตอบลงตาราง chat_logs ไว้ดูย้อนหลังและวัดผลในสัปดาห์ 3 */
@Repository
public class ChatLogRepository {

    private final JdbcTemplate jdbc;

    public ChatLogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(ChatReply reply) {
        Long[] sourceIds = reply.sources().stream().map(ChatReply.Source::chunkId).toArray(Long[]::new);
        Long[] citedIds = reply.sources().stream().filter(ChatReply.Source::cited).map(ChatReply.Source::chunkId).toArray(Long[]::new);
        String sql = """
                INSERT INTO chat_logs (question, answer, found, source_chunk_ids, cited_chunk_ids, model, retrieval_ms, llm_ms)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbc.update(sql, ps -> {
            ps.setString(1, reply.question());
            ps.setString(2, reply.answer());
            ps.setBoolean(3, reply.found());
            ps.setArray(4, ps.getConnection().createArrayOf("bigint", sourceIds));
            ps.setArray(5, ps.getConnection().createArrayOf("bigint", citedIds));
            ps.setString(6, reply.model());
            ps.setInt(7, (int) reply.retrievalMs());
            ps.setInt(8, (int) reply.llmMs());
        });
    }
}
