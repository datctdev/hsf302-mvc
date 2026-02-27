package com.hsf.e_comerce.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ChatConfig {

    @Bean
    @ConfigurationProperties(prefix = "app.chat")
    public ChatProperties chatProperties() {
        return new ChatProperties();
    }

    /** RestTemplate dành cho Ollama: read timeout dài (model 7B+ có thể cần 30–120s). */
    @Bean("ollamaRestTemplate")
    public RestTemplate ollamaRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);   // 10s
        factory.setReadTimeout(120_000);     // 120s (Ollama inference có thể rất lâu)
        return new RestTemplate(factory);
    }
}
