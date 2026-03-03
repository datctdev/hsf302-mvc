package com.hsf.e_comerce.shipping.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculateCartShippingFeeRequest {
    private Integer toDistrictId;
    private String toWardCode;
    /** Chỉ tính phí cho các món có ID trong list (null/empty = toàn bộ giỏ). */
    private List<UUID> cartItemIds;
}
