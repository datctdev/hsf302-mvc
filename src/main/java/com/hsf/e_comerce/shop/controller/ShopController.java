package com.hsf.e_comerce.shop.controller;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.shop.dto.request.UpdateShopRequest;
import com.hsf.e_comerce.shop.dto.response.ShopResponse;
import com.hsf.e_comerce.shop.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @GetMapping
    public ResponseEntity<ShopResponse> getMyShop(@CurrentUser User user) {
        ShopResponse response = shopService.getShopByUserId(user.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<ShopResponse> updateShop(
            @CurrentUser User user,
            @Valid @RequestBody UpdateShopRequest request) {
        ShopResponse response = shopService.updateShop(user.getId(), request);
        return ResponseEntity.ok(response);
    }
}
