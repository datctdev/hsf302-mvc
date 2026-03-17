package com.hsf.e_comerce.chatbot.controller;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.chatbot.dto.ChatbotInteractRequest;
import com.hsf.e_comerce.chatbot.dto.ChatbotResponseDto;
import com.hsf.e_comerce.chatbot.service.ChatbotService;
import com.hsf.e_comerce.common.annotation.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    /**
     * Initialize chatbot state (or return current). Called by frontend on widget load.
     */
    @GetMapping("/init")
    public ResponseEntity<ChatbotResponseDto> init(HttpSession session, @CurrentUser User principal) {
        ChatbotResponseDto dto = chatbotService.init(session, principal);
        return ResponseEntity.ok(dto);
    }

    /**
     * Handle button action or text input. Body: { "action": "..." } or { "text": "..." } or { "action": "SEARCH_CATEGORY", "categoryId": "uuid" }.
     */
    @PostMapping("/interact")
    public ResponseEntity<ChatbotResponseDto> interact(
            HttpSession session,
            @RequestBody ChatbotInteractRequest request,
            @CurrentUser User principal
    ) {
        ChatbotResponseDto dto = chatbotService.interact(session, request, principal);
        return ResponseEntity.ok(dto);
    }
}
