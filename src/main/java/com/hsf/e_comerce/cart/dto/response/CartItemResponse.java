package com.hsf.e_comerce.cart.dto.response;

import com.hsf.e_comerce.order.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    
    private UUID id;
    private UUID productId;
    private String productName;
    private String productImageUrl;
    private UUID variantId;
    private String variantName;
    private String variantValue;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CartItemResponse fromOrderItem(OrderItem item) {
        String imageUrl = null;

        if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
            imageUrl = item.getProduct()
                    .getImages()
                    .iterator()
                    .next()
                    .getImageUrl();
        }


        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProductName()) // ⚠ dùng snapshot trong order
                .productImageUrl(imageUrl)
                .variantId(item.getVariant() != null ? item.getVariant().getId() : null)
                .variantName(item.getVariantName())
                .variantValue(item.getVariantValue())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }


}
