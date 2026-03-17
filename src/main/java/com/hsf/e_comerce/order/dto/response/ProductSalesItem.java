package com.hsf.e_comerce.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Thống kê số lượng bán của một sản phẩm trong shop (seller).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSalesItem {
    private UUID productId;
    private String productName;
    private long quantitySold;
}
