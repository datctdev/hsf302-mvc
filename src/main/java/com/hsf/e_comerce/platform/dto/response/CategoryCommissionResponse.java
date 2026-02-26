package com.hsf.e_comerce.platform.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class CategoryCommissionResponse {

    private UUID categoryId;
    private String categoryName;
    private BigDecimal commissionRate;
}
