package com.chaiyanan09.thairag.chunk;

import java.util.UUID;

/** chunk ที่ค้นเจอ 1 ชิ้น score = cosine similarity (ยิ่งใกล้ 1 ยิ่งตรง) */
public record SearchHit(
        long chunkId,
        UUID documentId,
        String filename,
        int page,
        String content,
        double score
) {
}
