package com.hsf.e_comerce.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueSummaryResponse {

    private BigDecimal revenue;           // DELIVERED
    private BigDecimal estimatedRevenue;  // != REFUNDED & CANCELLED
}
