package com.hsf.e_comerce.platform.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class CommissionDetailResponse {

    private UUID orderId;
    private String sellerName;
    private BigDecimal orderAmount;
    private BigDecimal totalCommission;
    private List<CommissionItemResponse> items;
    private LocalDateTime createdAt;
}
