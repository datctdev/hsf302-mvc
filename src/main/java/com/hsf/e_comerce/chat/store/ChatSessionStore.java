package com.hsf.e_comerce.chat.store;

import com.hsf.e_comerce.chat.dto.ChatMessageDto;

import java.util.List;

public interface ChatSessionStore {
    void append(String sessionId, String role, String content);
    List<ChatMessageDto> getRecent(String sessionId, int maxMessages);
    void clear(String sessionId);
}
