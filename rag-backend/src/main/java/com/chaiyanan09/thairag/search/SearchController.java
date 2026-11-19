package com.chaiyanan09.thairag.search;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class SearchController {

    private final SearchService service;

    public SearchController(SearchService service) {
        this.service = service;
    }

    /**
     * ตัวอย่าง: GET /api/search?q=ลาพักการศึกษา&limit=5
     * ค้นเฉพาะบางไฟล์: เพิ่ม &documentIds=<uuid>&documentIds=<uuid> (eval ใช้ให้ค้นเฉพาะเอกสารของชุดคำถาม)
     */
    @GetMapping("/api/search")
    public SearchResponse search(@RequestParam("q") String q,
                                 @RequestParam(name = "limit", defaultValue = "5") int limit,
                                 @RequestParam(name = "documentIds", required = false) List<UUID> documentIds) {
        return service.search(q, limit, documentIds);
    }
}
