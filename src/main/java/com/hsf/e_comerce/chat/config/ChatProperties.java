
package com.hsf.e_comerce.chat.config;

import lombok.Data;

@Data
public class ChatProperties {
    /** Base URL Ollama (vd: http://localhost:11434). */
    private String ollamaBaseUrl = "";
    /** Model Ollama — mặc định qwen2.5:7b (đủ mạnh cho 16GB VRAM / 64GB RAM). Có thể đổi: llama3.1:8b, mistral:7b, phi3:medium. */
    private String ollamaModel = "qwen2.5:7b";
}
