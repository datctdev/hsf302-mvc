package com.hsf.e_comerce.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class CommissionOverviewResponse {

    private BigDecimal totalCommission;
    private Long totalOrders;
    private BigDecimal averageCommissionRate;
}
