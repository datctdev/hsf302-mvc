package com.hsf.e_comerce.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotInteractRequest {
    private String action;
    private String text;
    /** For SEARCH: categoryId when user selected a category. */
    private String categoryId;
}
