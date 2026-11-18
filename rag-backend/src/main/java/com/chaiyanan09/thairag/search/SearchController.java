package com.chaiyanan09.thairag.search;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {

    private final SearchService service;

    public SearchController(SearchService service) {
        this.service = service;
    }

    /** ตัวอย่าง: GET /api/search?q=ลาพักการศึกษา&limit=5 */
    @GetMapping("/api/search")
    public SearchResponse search(@RequestParam("q") String q,
                                 @RequestParam(name = "limit", defaultValue = "5") int limit) {
        return service.search(q, limit);
    }
}
