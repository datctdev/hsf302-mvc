package com.hsf.e_comerce.order.dto.request;

import com.hsf.e_comerce.order.dto.response.OrderResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderRequest {

    @NotBlank
    private String shippingName;

    @NotBlank
    private String shippingPhone;

    @NotBlank
    private String shippingAddress;

    private Integer shippingDistrictId;
    private String shippingWardCode;
    private String shippingCity;

    @NotNull
    private BigDecimal shippingFee;

    private String notes;

    public static UpdateOrderRequest fromOrder(OrderResponse order) {
        return new UpdateOrderRequest(
                order.getShippingName(),
                order.getShippingPhone(),
                order.getShippingAddress(),
                order.getShippingDistrictId(),
                order.getShippingWardCode(),
                order.getShippingCity(),
                order.getShippingFee(),
                order.getNotes()
        );
    }
}
