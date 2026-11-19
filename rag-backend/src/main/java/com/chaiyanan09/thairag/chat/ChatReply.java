package com.chaiyanan09.thairag.chat;

import java.util.List;
import java.util.UUID;

/**
 * คำตอบที่ส่งให้หน้าเว็บ
 *
 * @param found   false เมื่อเอกสารไม่มีคำตอบ (หน้าเว็บจะแสดงต่างจากคำตอบปกติ)
 * @param sources chunk ที่ส่งให้ LLM อ่าน เรียงตามเลขอ้างอิง [1], [2], ...
 */
public record ChatReply(
        String question,
        String answer,
        boolean found,
        List<Source> sources,
        String model,
        long retrievalMs,
        long llmMs
) {

    /** @param cited true ถ้าคำตอบอ้างถึงชิ้นนี้ */
    public record Source(
            int n,
            long chunkId,
            UUID documentId,
            String filename,
            int page,
            String content,
            double score,
            boolean cited
    ) {
    }
}
