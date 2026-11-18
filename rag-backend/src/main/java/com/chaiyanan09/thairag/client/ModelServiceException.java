package com.chaiyanan09.thairag.client;

/** model service ล่ม เรียกไม่ถึง หรือตอบผิดรูปแบบ (API ตอบ 502 Bad Gateway) */
public class ModelServiceException extends RuntimeException {

    public ModelServiceException(String message) {
        super(message);
    }

    public ModelServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
