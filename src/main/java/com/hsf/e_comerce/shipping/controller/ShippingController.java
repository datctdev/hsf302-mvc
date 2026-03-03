package com.hsf.e_comerce.shipping.controller;

import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.shipping.dto.request.CalculateCartShippingFeeRequest;
import com.hsf.e_comerce.shipping.dto.request.CalculateShippingFeeRequest;
import com.hsf.e_comerce.shipping.dto.response.CalculateShippingFeeResponse;
import com.hsf.e_comerce.shipping.dto.response.CartShippingFeeResponse;
import com.hsf.e_comerce.shipping.dto.response.GHNProvinceResponse;
import com.hsf.e_comerce.shipping.dto.response.GHNDistrictResponse;
import com.hsf.e_comerce.shipping.dto.response.GHNWardResponse;
import com.hsf.e_comerce.shipping.service.GHNService;
import com.hsf.e_comerce.shipping.service.ShippingService;
import com.hsf.e_comerce.auth.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;
    private final GHNService ghnService;

    @PostMapping("/calculate-fee")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CalculateShippingFeeResponse> calculateShippingFee(
            @CurrentUser User currentUser,
            @Valid @RequestBody CalculateShippingFeeRequest request) {
        
        CalculateShippingFeeResponse response = shippingService.calculateShippingFee(
                currentUser.getId(), request);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Tính phí vận chuyển cho toàn bộ giỏ (nhiều shop): mỗi shop một phí GHN, trả về tổng + chi tiết.
     * Dùng cho trang checkout "Đặt tất cả".
     */
    @PostMapping("/calculate-fee-cart")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CartShippingFeeResponse> calculateCartShippingFee(
            @CurrentUser User currentUser,
            @RequestBody CalculateCartShippingFeeRequest request) {
        CartShippingFeeResponse response = shippingService.calculateShippingFeesForCart(
                currentUser.getId(),
                request.getToDistrictId(),
                request.getToWardCode(),
                request.getCartItemIds());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/provinces")
    public ResponseEntity<List<GHNProvinceResponse>> getProvinces() {
        List<GHNProvinceResponse> provinces = ghnService.getProvinces();
        return ResponseEntity.ok(provinces);
    }

    @GetMapping("/districts")
    public ResponseEntity<List<GHNDistrictResponse>> getDistricts(@RequestParam Integer provinceId) {
        List<GHNDistrictResponse> districts = ghnService.getDistricts(provinceId);
        return ResponseEntity.ok(districts);
    }

    @GetMapping("/wards")
    public ResponseEntity<List<GHNWardResponse>> getWards(@RequestParam Integer districtId) {
        List<GHNWardResponse> wards = ghnService.getWards(districtId);
        return ResponseEntity.ok(wards);
    }
}
