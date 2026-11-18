package com.chaiyanan09.thairag.web;

import com.chaiyanan09.thairag.client.ModelServiceClient;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** เช็กว่า backend, ฐานข้อมูล และ model service พร้อมหรือไม่ (หน้าเว็บใช้แสดงสถานะ) */
@RestController
public class HealthController {

    private final JdbcTemplate jdbc;
    private final ModelServiceClient modelService;

    public HealthController(JdbcTemplate jdbc, ModelServiceClient modelService) {
        this.jdbc = jdbc;
        this.modelService = modelService;
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("status", "ok");
        result.put("database", databaseStatus());
        result.put("modelService", modelService.isHealthy() ? "ok" : "down");
        return result;
    }

    private String databaseStatus() {
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return "ok";
        } catch (DataAccessException e) {
            return "down";
        }
    }
}
