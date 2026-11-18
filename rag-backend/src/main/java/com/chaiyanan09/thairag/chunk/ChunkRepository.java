package com.chaiyanan09.thairag.chunk;

import com.chaiyanan09.thairag.client.IngestResult;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * ตาราง chunks ใช้ SQL ตรงๆ แทน JPA เพราะคอลัมน์ vector และ operator <=> ของ pgvector
 * เขียนเป็น SQL แล้วอ่านง่ายกว่า และจะต่อยอดเป็น hybrid search ในสัปดาห์ 3 ได้ตรงๆ
 */
@Repository
public class ChunkRepository {

    private final JdbcTemplate jdbc;
    private final JdbcClient client;

    public ChunkRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.client = JdbcClient.create(jdbc);
    }

    public void insertAll(UUID documentId, List<IngestResult.Chunk> chunks, List<float[]> vectors) {
        if (chunks.size() != vectors.size()) {
            throw new IllegalArgumentException("chunks and vectors must have the same size");
        }
        String sql = """
                INSERT INTO chunks (document_id, chunk_index, page_number, content, content_tokens, embedding)
                VALUES (?, ?, ?, ?, ?, CAST(? AS vector))
                """;
        jdbc.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                IngestResult.Chunk chunk = chunks.get(i);
                ps.setObject(1, documentId);
                ps.setInt(2, chunk.index());
                ps.setInt(3, chunk.page());
                ps.setString(4, chunk.text());
                ps.setString(5, chunk.tokens() == null ? "" : chunk.tokens());
                ps.setString(6, PgVector.format(vectors.get(i)));
            }

            @Override
            public int getBatchSize() {
                return chunks.size();
            }
        });
    }

    /** หา chunk ที่ความหมายใกล้คำถามที่สุด (<=> คือ cosine distance, ใช้ HNSW index) */
    public List<SearchHit> search(float[] queryVector, int limit) {
        String sql = """
                SELECT c.id, c.document_id, d.filename, c.page_number, c.content,
                       1 - (c.embedding <=> CAST(:query AS vector)) AS score
                FROM chunks c
                JOIN documents d ON d.id = c.document_id
                WHERE d.status = 'READY'
                ORDER BY c.embedding <=> CAST(:query AS vector)
                LIMIT :limit
                """;
        return client.sql(sql)
                .param("query", PgVector.format(queryVector))
                .param("limit", limit)
                .query((rs, rowNum) -> new SearchHit(
                        rs.getLong("id"),
                        rs.getObject("document_id", UUID.class),
                        rs.getString("filename"),
                        rs.getInt("page_number"),
                        rs.getString("content"),
                        rs.getDouble("score")))
                .list();
    }
}
