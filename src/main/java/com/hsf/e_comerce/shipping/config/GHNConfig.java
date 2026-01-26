package com.hsf.e_comerce.shipping.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "ghn.api")
@Data
public class GHNConfig {
    private String url = "https://dev-online-gateway.ghn.vn";
    private String token;
    private Integer shopId;

    @PostConstruct
    public void validate() {
        if (token == null || token.isEmpty() || token.equals("YOUR_TOKEN_HERE")) {
            log.warn("⚠️ GHN Token chưa được cấu hình. Vui lòng set biến môi trường GHN_TOKEN.");
        } else {
            log.info("✓ GHN Token đã được cấu hình (length: {})", token.length());
        }
        
        if (shopId == null || shopId == 0) {
            log.warn("⚠️ GHN Shop ID chưa được cấu hình. Vui lòng set biến môi trường GHN_SHOP_ID.");
        } else {
            log.info("✓ GHN Shop ID đã được cấu hình: {}", shopId);
        }
        
        log.info("GHN API URL: {}", url);
    }
}
