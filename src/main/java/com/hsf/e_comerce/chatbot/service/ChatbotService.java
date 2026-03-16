package com.hsf.e_comerce.chatbot.service;

import com.hsf.e_comerce.chatbot.dto.ChatbotResponseDto;
import com.hsf.e_comerce.chatbot.dto.ChatbotInteractRequest;
import com.hsf.e_comerce.auth.entity.User;

import jakarta.servlet.http.HttpSession;
import java.util.List;

public interface ChatbotService {

    /**
     * Initialize or return current state. Sets session chatbotCurrentNode and chatbotRootNode.
     */
    ChatbotResponseDto init(HttpSession session, User principal);

    /**
     * Handle button action or text input. Updates session state and returns next message/options.
     */
    ChatbotResponseDto interact(HttpSession session, ChatbotInteractRequest request, User principal);
}
