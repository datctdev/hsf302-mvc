package com.hsf.e_comerce.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@ConfigurationProperties(prefix = "vnpt.ekyc")
@Getter
@Setter
public class EKycConfig {
    private String baseUrl;
    private String accessToken;
    private String tokenId;
    private String tokenKey;
    private String macAddress;

    @Bean
    public RestClient vnptRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                // Don't use defaultHeader - each endpoint has different auth requirements
                .build();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
