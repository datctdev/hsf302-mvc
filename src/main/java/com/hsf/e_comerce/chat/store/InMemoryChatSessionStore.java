package com.hsf.e_comerce.chat.store;

import com.hsf.e_comerce.chat.dto.ChatMessageDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryChatSessionStore implements ChatSessionStore {

    private static final int MAX_MESSAGES_PER_SESSION = 50;

    private final Map<String, List<ChatMessageDto>> sessions = new ConcurrentHashMap<>();

    @Override
    public void append(String sessionId, String role, String content) {
        if (sessionId == null || sessionId.isBlank()) return;
        sessions.compute(sessionId, (k, list) -> {
            List<ChatMessageDto> messages = list != null ? list : new ArrayList<>();
            messages.add(new ChatMessageDto(role, content != null ? content : ""));
            if (messages.size() > MAX_MESSAGES_PER_SESSION) {
                messages = new ArrayList<>(messages.subList(messages.size() - MAX_MESSAGES_PER_SESSION, messages.size()));
            }
            return messages;
        });
    }

    @Override
    public List<ChatMessageDto> getRecent(String sessionId, int maxMessages) {
        if (sessionId == null || sessionId.isBlank()) return List.of();
        List<ChatMessageDto> list = sessions.get(sessionId);
        if (list == null || list.isEmpty()) return List.of();
        int from = Math.max(0, list.size() - maxMessages);
        return List.copyOf(list.subList(from, list.size()));
    }

    @Override
    public void clear(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) sessions.remove(sessionId);
    }
}
