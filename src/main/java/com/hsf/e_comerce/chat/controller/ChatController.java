package com.hsf.e_comerce.chat.controller;

import com.hsf.e_comerce.chat.dto.ChatResponse;
import com.hsf.e_comerce.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody Map<String, Object> body) {
        String message = body != null && body.get("message") != null ? body.get("message").toString().trim() : "";
        if (message.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (message.length() > 2000) {
            message = message.substring(0, 2000);
        }
        ChatResponse response = chatService.chat(message);
        return ResponseEntity.ok(response);
    }
}
