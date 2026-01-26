package com.hsf.e_comerce.order.dto.request;

import com.hsf.e_comerce.order.dto.response.OrderResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    
    @NotNull(message = "Shop ID is required")
    private java.util.UUID shopId;
    
    @NotBlank(message = "Shipping name is required")
    private String shippingName;
    
    @NotBlank(message = "Shipping phone is required")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Phone number must be 10-11 digits")
    private String shippingPhone;
    
    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;
    
    private String shippingCity;
    
    private String shippingDistrict;
    
    private String shippingWard;
    
    private Integer shippingDistrictId;  // Mã quận/huyện GHN
    
    private String shippingWardCode;  // Mã phường/xã GHN
    
    private String notes;
    
    @NotNull(message = "Shipping fee is required")
    private BigDecimal shippingFee = java.math.BigDecimal.ZERO;

    public static CreateOrderRequest fromOrder(OrderResponse order) {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setShopId(order.getShopId());
        req.setShippingName(order.getShippingName());
        req.setShippingPhone(order.getShippingPhone());
        req.setShippingAddress(order.getShippingAddress());
        req.setShippingCity(order.getShippingCity());
        req.setShippingDistrict(order.getShippingDistrict());
        req.setShippingWard(order.getShippingWard());
        req.setShippingDistrictId(order.getShippingDistrictId());
        req.setShippingWardCode(order.getShippingWardCode());
        req.setNotes(order.getNotes());
        req.setShippingFee(order.getShippingFee());
        return req;
    }


}
