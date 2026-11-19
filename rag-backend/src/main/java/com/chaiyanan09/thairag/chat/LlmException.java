package com.chaiyanan09.thairag.chat;

/** เรียก LLM (Typhoon) ไม่สำเร็จ เช่น key ผิด เน็ตหลุด หรือเกิน rate limit (API ตอบ 502) */
public class LlmException extends RuntimeException {

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
