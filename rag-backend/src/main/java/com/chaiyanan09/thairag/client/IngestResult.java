package com.chaiyanan09.thairag.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** คำตอบจาก POST /ingest ของ model service (Python ใช้ชื่อแบบ snake_case) */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IngestResult(
        String filename,
        @JsonProperty("page_count") int pageCount,
        List<Chunk> chunks,
        @JsonProperty("empty_pages") List<Integer> emptyPages
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Chunk(int index, int page, String text, String tokens) {
    }
}
