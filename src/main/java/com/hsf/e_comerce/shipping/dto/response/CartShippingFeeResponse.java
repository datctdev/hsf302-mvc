package com.hsf.e_comerce.shipping.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartShippingFeeResponse {
    private boolean success;
    private String message;
    /** Tổng phí vận chuyển (tất cả shop). */
    private BigDecimal totalFee;
    /** Phí từng shop (để hiển thị / đối chiếu). */
    private List<ShopFeeItem> feesByShop;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShopFeeItem {
        private String shopId;
        private BigDecimal shippingFee;
    }
}
