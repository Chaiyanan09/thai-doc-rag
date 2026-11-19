package com.chaiyanan09.thairag.chat;

import java.util.List;
import java.util.UUID;

/**
 * @param question    คำถาม
 * @param documentIds ไฟล์ที่แนบในแชทนี้ ถ้าว่างหรือไม่ส่งมา จะค้นจากทุกเอกสารในระบบ
 */
public record ChatRequest(String question, List<UUID> documentIds) {
}
