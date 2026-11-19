package com.chaiyanan09.thairag.chat;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService service;

    public ChatController(ChatService service) {
        this.service = service;
    }

    /** ตัวอย่าง body: {"question": "ลาพักการศึกษาต้องทำอย่างไร"} */
    @PostMapping
    public ChatReply ask(@RequestBody ChatRequest request) {
        return service.ask(request.question());
    }

    /** ค่าตั้งค่าของการตอบที่ใช้อยู่ (eval/run_eval.py เรียกเพื่อบันทึกว่ารอบนี้ทดลองด้วยค่าอะไร) */
    @GetMapping("/settings")
    public ChatSettings settings() {
        return service.settings();
    }
}
