package com.hsf.e_comerce.chat.controller;

import com.hsf.e_comerce.chat.dto.ChatRequest;
import com.hsf.e_comerce.chat.dto.ChatResponse;
import com.hsf.e_comerce.chat.service.ChatService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<?> chat(@RequestBody @Valid ChatRequest request, HttpSession session) {
        String sessionId = session != null ? session.getId() : null;
        ChatResponse response = chatService.chat(sessionId, request.getMessage());
        return ResponseEntity.ok(response);
    }

    /**
     * Streaming endpoint: SSE. Client gửi POST với body { "message": "..." }, nhận event stream:
     * - event "chunk": data = đoạn text (reply đang gõ)
     * - event "done": data = JSON { "reply": "...", "productSuggestions": [...] }
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody @Valid ChatRequest request, HttpSession session) {
        String sessionId = session != null ? session.getId() : null;
        SseEmitter emitter = new SseEmitter(120_000L);
        chatService.streamChat(sessionId, request.getMessage(), emitter);
        return emitter;
    }
}
