package com.hsf.e_comerce.platform.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CommissionResponse {

    private UUID orderId;
    private UUID sellerId;
    private String sellerName;
    private BigDecimal orderAmount;
    private BigDecimal commissionAmount;
    private LocalDateTime createdAt;
}
