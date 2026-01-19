package com.hsf.e_comerce.shipping.service;

import com.hsf.e_comerce.cart.entity.Cart;
import com.hsf.e_comerce.cart.entity.CartItem;
import com.hsf.e_comerce.cart.repository.CartRepository;
import com.hsf.e_comerce.shipping.dto.request.CalculateShippingFeeRequest;
import com.hsf.e_comerce.shipping.dto.request.GHNCalculateFeeRequest;
import com.hsf.e_comerce.shipping.dto.response.CalculateShippingFeeResponse;
import com.hsf.e_comerce.shipping.dto.response.GHNCalculateFeeResponse;
import com.hsf.e_comerce.shop.entity.Shop;
import com.hsf.e_comerce.shop.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingService {

    private final GHNService ghnService;
    private final CartRepository cartRepository;
    private final ShopRepository shopRepository;

    /**
     * Tính phí vận chuyển từ cart
     */
    public CalculateShippingFeeResponse calculateShippingFee(UUID userId, CalculateShippingFeeRequest request) {
        try {
            // Lấy cart của user
            Cart cart = cartRepository.findByUserIdWithItemsAndProducts(userId)
                    .orElseThrow(() -> new RuntimeException("Giỏ hàng trống"));

            if (cart.getItems() == null || cart.getItems().isEmpty()) {
                return CalculateShippingFeeResponse.builder()
                        .success(false)
                        .message("Giỏ hàng trống")
                        .shippingFee(BigDecimal.ZERO)
                        .build();
            }

            // Lấy shop
            Shop shop = shopRepository.findById(UUID.fromString(request.getShopId()))
                    .orElseThrow(() -> new RuntimeException("Shop không tồn tại"));

            // Tính tổng cân nặng từ cart
            int totalWeight = request.getWeight() != null ? request.getWeight() : calculateTotalWeight(cart);

            // Lấy địa chỉ shop (tạm thời dùng giá trị mặc định hoặc từ config)
            // TODO: Cần thêm district_id và ward_code vào Shop entity
            Integer fromDistrictId = getShopDistrictId(shop);
            String fromWardCode = getShopWardCode(shop);

            if (fromDistrictId == null || fromWardCode == null) {
                log.warn("Shop chưa có địa chỉ đầy đủ (district_id, ward_code). Sử dụng giá trị mặc định.");
                // Giá trị mặc định cho HCM (Quận 1, Phường Bến Nghé)
                fromDistrictId = 1442; // Quận 1, HCM
                fromWardCode = "21012";  // Phường Bến Nghé (String)
            }

            // Kiểm tra địa chỉ giao hàng
            if (request.getToDistrictId() == null || request.getToWardCode() == null) {
                return CalculateShippingFeeResponse.builder()
                        .success(false)
                        .message("Vui lòng nhập đầy đủ địa chỉ giao hàng (Quận/Huyện và Phường/Xã)")
                        .shippingFee(BigDecimal.ZERO)
                        .build();
            }

            // Gọi GHN API tính phí
            GHNCalculateFeeRequest ghnRequest = GHNCalculateFeeRequest.builder()
                    .from_district_id(fromDistrictId)
                    .from_ward_code(fromWardCode)  // Đã là String
                    .to_district_id(request.getToDistrictId())
                    .to_ward_code(request.getToWardCode())  // Đã là String, không cần parse
                    .weight(totalWeight)
                    .service_type_id(2) // Hàng nhẹ
                    .build();

            GHNCalculateFeeResponse ghnResponse = ghnService.calculateFee(ghnRequest);

            return CalculateShippingFeeResponse.builder()
                    .success(true)
                    .shippingFee(BigDecimal.valueOf(ghnResponse.getTotal()))
                    .message("Tính phí thành công")
                    .build();

        } catch (Exception e) {
            log.error("Error calculating shipping fee", e);
            return CalculateShippingFeeResponse.builder()
                    .success(false)
                    .message("Không thể tính phí vận chuyển: " + e.getMessage())
                    .shippingFee(BigDecimal.ZERO)
                    .build();
        }
    }

    /**
     * Tính tổng cân nặng từ cart
     */
    private int calculateTotalWeight(Cart cart) {
        int totalWeight = 0;
        for (CartItem item : cart.getItems()) {
            int itemWeight = item.getProduct().getWeight() != null ? item.getProduct().getWeight() : 500;
            totalWeight += itemWeight * item.getQuantity();
        }
        return totalWeight;
    }

    /**
     * Lấy district_id của shop
     */
    private Integer getShopDistrictId(Shop shop) {
        return shop.getDistrictId();
    }

    /**
     * Lấy ward_code của shop
     */
    private String getShopWardCode(Shop shop) {
        return shop.getWardCode();
    }
}
