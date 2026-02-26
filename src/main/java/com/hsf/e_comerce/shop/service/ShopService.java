package com.hsf.e_comerce.shop.service;

import aj.org.objectweb.asm.commons.Remapper;
import com.hsf.e_comerce.shop.dto.request.UpdateShopRequest;
import com.hsf.e_comerce.shop.dto.response.ShopResponse;
import com.hsf.e_comerce.shop.entity.Shop;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShopService {
    
    ShopResponse getShopByUserId(UUID userId);
    
    ShopResponse updateShop(UUID userId, UpdateShopRequest request);
    
    boolean hasShop(UUID userId);

    Optional<Shop> getShop(UUID shopId);

    long count();

    List<ShopResponse> findAllShops();

    List<Shop> getAllShop();

    String findByUserId(UUID sellerId);

    List<UUID> findAllByNameContaining(String sellerName);
}
