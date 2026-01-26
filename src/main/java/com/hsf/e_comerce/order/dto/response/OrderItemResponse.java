package com.hsf.e_comerce.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    
    private UUID id;
    private UUID productId;
    private UUID variantId;
    private String productName;
    private String variantName;
    private String variantValue;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String productImageUrl; // Thumbnail hoặc first image
    private boolean isReviewed;
}
