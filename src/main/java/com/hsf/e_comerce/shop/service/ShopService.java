package com.hsf.e_comerce.shop.service;

import com.hsf.e_comerce.shop.dto.request.UpdateShopRequest;
import com.hsf.e_comerce.shop.dto.response.ShopResponse;

import java.util.UUID;

public interface ShopService {
    
    ShopResponse getShopByUserId(UUID userId);
    
    ShopResponse updateShop(UUID userId, UpdateShopRequest request);
    
    boolean hasShop(UUID userId);
}
