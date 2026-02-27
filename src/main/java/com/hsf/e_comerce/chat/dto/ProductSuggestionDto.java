package com.hsf.e_comerce.chat.dto;

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
public class ProductSuggestionDto {
    private UUID id;
    private String name;
    private BigDecimal basePrice;
    private String productUrl;
    private String categoryName;
    /** URL ảnh đại diện (để hiển thị trong gợi ý). */
    private String thumbnailUrl;
}
