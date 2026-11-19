package com.chaiyanan09.thairag.search;

import com.chaiyanan09.thairag.chunk.ChunkRepository;
import com.chaiyanan09.thairag.client.ModelServiceClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** สัปดาห์ 1: ค้นด้วย vector อย่างเดียว (สัปดาห์ 2 ส่งผลนี้ให้ LLM, สัปดาห์ 3 เพิ่ม keyword เป็น hybrid) */
@Service
public class SearchService {

    public static final int MAX_LIMIT = 20;

    private final ChunkRepository chunks;
    private final ModelServiceClient modelService;

    public SearchService(ChunkRepository chunks, ModelServiceClient modelService) {
        this.chunks = chunks;
        this.modelService = modelService;
    }

    /** documentIds ว่างหรือ null = ค้นทุกเอกสาร */
    public SearchResponse search(String query, int limit, List<UUID> documentIds) {
        String q = query == null ? "" : query.strip();
        if (q.isEmpty()) {
            throw new IllegalArgumentException("กรุณาพิมพ์คำค้นหา");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit ต้องอยู่ระหว่าง 1 ถึง " + MAX_LIMIT);
        }
        float[] queryVector = modelService.embed(List.of(q)).get(0);
        return new SearchResponse(q, chunks.search(queryVector, limit, documentIds));
    }
}
