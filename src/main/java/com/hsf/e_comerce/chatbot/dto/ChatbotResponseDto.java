package com.hsf.e_comerce.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotResponseDto {
    private String messageText;
    private List<ChatbotOptionDto> options;
    private List<ChatbotProductCardDto> productCards;
    private boolean humanHandoffRequired;
    /** When humanHandoffRequired is true, client should use this to subscribe for replies: /user/{liveChatSessionId}/queue/chat */
    private String liveChatSessionId;
    private boolean inputExpected;
    private String inputHint;
}
