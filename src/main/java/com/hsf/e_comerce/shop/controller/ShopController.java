package com.hsf.e_comerce.shop.controller;

import com.hsf.e_comerce.auth.service.UserService;
import com.hsf.e_comerce.common.exception.CustomException;
import com.hsf.e_comerce.shop.dto.request.UpdateShopRequest;
import com.hsf.e_comerce.shop.dto.response.ShopResponse;
import com.hsf.e_comerce.shop.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ShopResponse> getMyShop(Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        ShopResponse response = shopService.getShopByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<ShopResponse> updateShop(
            @Valid @RequestBody UpdateShopRequest request,
            Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        ShopResponse response = shopService.updateShop(userId, request);
        return ResponseEntity.ok(response);
    }

    private UUID getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
            throw new CustomException("Không thể xác định người dùng");
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        return userService.findByEmail(email).getId();
    }
}
