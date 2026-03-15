package com.hsf.e_comerce.shipping.service;

import com.hsf.e_comerce.cart.entity.Cart;
import com.hsf.e_comerce.cart.entity.CartItem;
import com.hsf.e_comerce.cart.repository.CartRepository;
import com.hsf.e_comerce.shipping.dto.request.CalculateShippingFeeRequest;
import com.hsf.e_comerce.shipping.dto.request.GHNCalculateFeeRequest;
import com.hsf.e_comerce.shipping.dto.response.CalculateShippingFeeResponse;
import com.hsf.e_comerce.shipping.dto.response.CartShippingFeeResponse;
import com.hsf.e_comerce.shipping.dto.response.GHNCalculateFeeResponse;
import com.hsf.e_comerce.shop.entity.Shop;
import com.hsf.e_comerce.shop.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
     * Tính cân nặng từ danh sách cart item (một shop).
     */
    private int calculateWeightFromItems(List<CartItem> items) {
        int total = 0;
        for (CartItem item : items) {
            int w = item.getProduct().getWeight() != null ? item.getProduct().getWeight() : 500;
            total += w * item.getQuantity();
        }
        return total;
    }

    /**
     * Tính phí GHN cho một shop (từ địa chỉ shop → địa chỉ giao hàng, cân nặng = sản phẩm của shop đó).
     * Nếu GHN lỗi hoặc địa chỉ thiếu thì trả về 0 và log.
     */
    public BigDecimal calculateShippingFeeForShop(Shop shop, List<CartItem> items,
                                                   Integer toDistrictId, String toWardCode) {
        if (toDistrictId == null || toWardCode == null || toWardCode.isBlank()) {
            log.warn("Địa chỉ giao hàng thiếu (toDistrictId, toWardCode). Shop {}", shop.getId());
            return BigDecimal.ZERO;
        }
        Integer fromDistrictId = getShopDistrictId(shop);
        String fromWardCode = getShopWardCode(shop);
        if (fromDistrictId == null || fromWardCode == null || fromWardCode.isBlank()) {
            log.warn("Shop {} chưa có district_id/ward_code. Dùng mặc định HCM.", shop.getId());
            fromDistrictId = 1442;
            fromWardCode = "21012";
        }
        int weight = items.isEmpty() ? 500 : calculateWeightFromItems(items);
        try {
            GHNCalculateFeeRequest ghnRequest = GHNCalculateFeeRequest.builder()
                    .from_district_id(fromDistrictId)
                    .from_ward_code(fromWardCode)
                    .to_district_id(toDistrictId)
                    .to_ward_code(toWardCode)
                    .weight(weight)
                    .service_type_id(2)
                    .build();
            GHNCalculateFeeResponse ghnResponse = ghnService.calculateFee(ghnRequest);
            return BigDecimal.valueOf(ghnResponse.getTotal());
        } catch (Exception e) {
            log.error("Lỗi tính phí GHN cho shop {}: {}", shop.getId(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Tính phí vận chuyển cho giỏ (nhiều shop): mỗi shop một phí GHN riêng.
     * Nếu cartItemIds != null và không rỗng thì chỉ tính cho các món có ID trong list (thuộc giỏ của user).
     */
    public CartShippingFeeResponse calculateShippingFeesForCart(UUID userId,
                                                                Integer toDistrictId, String toWardCode,
                                                                List<UUID> cartItemIds) {
        try {
            Cart cart = cartRepository.findByUserIdWithItemsAndProducts(userId)
                    .orElseThrow(() -> new RuntimeException("Giỏ hàng trống"));
            if (cart.getItems() == null || cart.getItems().isEmpty()) {
                return CartShippingFeeResponse.builder()
                        .success(false)
                        .message("Giỏ hàng trống")
                        .totalFee(BigDecimal.ZERO)
                        .feesByShop(List.of())
                        .build();
            }
            List<CartItem> itemsToUse = new ArrayList<>(cart.getItems());
            if (cartItemIds != null && !cartItemIds.isEmpty()) {
                itemsToUse = cart.getItems().stream()
                        .filter(item -> cartItemIds.contains(item.getId()))
                        .toList();
                if (itemsToUse.isEmpty()) {
                    return CartShippingFeeResponse.builder()
                            .success(false)
                            .message("Không có món nào được chọn")
                            .totalFee(BigDecimal.ZERO)
                            .feesByShop(List.of())
                            .build();
                }
            }
            if (toDistrictId == null || toWardCode == null || toWardCode.isBlank()) {
                return CartShippingFeeResponse.builder()
                        .success(false)
                        .message("Vui lòng chọn đầy đủ Quận/Huyện và Phường/Xã")
                        .totalFee(BigDecimal.ZERO)
                        .feesByShop(List.of())
                        .build();
            }
            Map<Shop, List<CartItem>> byShop = itemsToUse.stream()
                    .collect(Collectors.groupingBy(item -> item.getProduct().getShop(), LinkedHashMap::new, Collectors.toList()));
            List<CartShippingFeeResponse.ShopFeeItem> feesByShop = new ArrayList<>();
            BigDecimal totalFee = BigDecimal.ZERO;
            for (Map.Entry<Shop, List<CartItem>> entry : byShop.entrySet()) {
                BigDecimal fee = calculateShippingFeeForShop(entry.getKey(), entry.getValue(), toDistrictId, toWardCode);
                totalFee = totalFee.add(fee);
                feesByShop.add(new CartShippingFeeResponse.ShopFeeItem(entry.getKey().getId().toString(), fee));
            }
            return CartShippingFeeResponse.builder()
                    .success(true)
                    .message("Tính phí thành công")
                    .totalFee(totalFee)
                    .feesByShop(feesByShop)
                    .build();
        } catch (Exception e) {
            log.error("Error calculating cart shipping fees", e);
            return CartShippingFeeResponse.builder()
                    .success(false)
                    .message("Không thể tính phí: " + e.getMessage())
                    .totalFee(BigDecimal.ZERO)
                    .feesByShop(List.of())
                    .build();
        }
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
