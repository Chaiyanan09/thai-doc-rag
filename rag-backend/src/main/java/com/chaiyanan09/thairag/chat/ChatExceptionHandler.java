package com.chaiyanan09.thairag.chat;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** ตอบ error ของ LLM ในรูปแบบเดียวกับ ApiExceptionHandler: {"message": "..."} */
@RestControllerAdvice
public class ChatExceptionHandler {

    @ExceptionHandler(LlmException.class)
    public ResponseEntity<Map<String, String>> llm(LlmException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", e.getMessage()));
    }
}
