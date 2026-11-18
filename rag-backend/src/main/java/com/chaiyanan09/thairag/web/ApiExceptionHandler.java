package com.chaiyanan09.thairag.web;

import com.chaiyanan09.thairag.client.ModelServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** แปลง exception เป็น JSON รูปแบบเดียวกันหมด: {"message": "..."} ให้หน้าเว็บแสดงได้ง่าย */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(ModelServiceException.class)
    public ResponseEntity<Map<String, String>> modelService(ModelServiceException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("message", "ไฟล์ใหญ่เกิน 30 MB"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> status(ResponseStatusException e) {
        String reason = e.getReason() == null ? e.getStatusCode().toString() : e.getReason();
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", reason));
    }
}
