package com.hsf.e_comerce.shop.service.impl;

import com.hsf.e_comerce.common.exception.CustomException;
import com.hsf.e_comerce.shop.dto.request.UpdateShopRequest;
import com.hsf.e_comerce.shop.dto.response.ShopResponse;
import com.hsf.e_comerce.shop.entity.Shop;
import com.hsf.e_comerce.shop.repository.ShopRepository;
import com.hsf.e_comerce.shop.service.ShopService;
import com.hsf.e_comerce.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopRepository shopRepository;
    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public ShopResponse getShopByUserId(UUID userId) {
        Shop shop = shopRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException("Không tìm thấy shop của bạn."));
        return mapToResponse(shop);
    }

    @Override
    @Transactional
    public ShopResponse updateShop(UUID userId, UpdateShopRequest request) {
        Shop shop = shopRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException("Không tìm thấy shop của bạn."));

        // Kiểm tra tên shop mới có trùng không (nếu đổi tên)
        if (request.getName() != null && !request.getName().equals(shop.getName())) {
            if (shopRepository.existsByName(request.getName())) {
                throw new CustomException("Tên shop đã tồn tại. Vui lòng chọn tên khác.");
            }
        }

        // Cập nhật thông tin
        if (request.getName() != null) {
            shop.setName(request.getName());
        }
        if (request.getDescription() != null) {
            shop.setDescription(request.getDescription());
        }
        if (request.getPhoneNumber() != null) {
            shop.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAddress() != null) {
            shop.setAddress(request.getAddress());
        }
        if (request.getLogoUrl() != null) {
            shop.setLogoUrl(request.getLogoUrl());
        }
        if (request.getCoverImageUrl() != null) {
            shop.setCoverImageUrl(request.getCoverImageUrl());
        }

        shop = shopRepository.save(shop);
        return mapToResponse(shop);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasShop(UUID userId) {
        return shopRepository.existsByUserId(userId);
    }

    private ShopResponse mapToResponse(Shop shop) {
        return ShopResponse.builder()
                .id(shop.getId())
                .userId(shop.getUser().getId())
                .name(shop.getName())
                .description(shop.getDescription())
                .logoUrl(shop.getLogoUrl())
                .coverImageUrl(shop.getCoverImageUrl())
                .phoneNumber(shop.getPhoneNumber())
                .address(shop.getAddress())
                .status(shop.getStatus().getCode())
                .averageRating(shop.getAverageRating())
                .createdAt(shop.getCreatedAt())
                .updatedAt(shop.getUpdatedAt())
                .build();
    }
}
