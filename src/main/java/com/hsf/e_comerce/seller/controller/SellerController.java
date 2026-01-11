package com.hsf.e_comerce.seller.controller;

import com.hsf.e_comerce.auth.service.UserService;
import com.hsf.e_comerce.common.dto.response.MessageResponse;
import com.hsf.e_comerce.common.exception.CustomException;
import com.hsf.e_comerce.seller.dto.request.SellerRequestRequest;
import com.hsf.e_comerce.seller.dto.response.SellerRequestResponse;
import com.hsf.e_comerce.seller.service.SellerRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class SellerController {

    private final SellerRequestService sellerRequestService;
    private final UserService userService;

    @PostMapping("/request")
    public ResponseEntity<SellerRequestResponse> createRequest(
            @Valid @RequestBody SellerRequestRequest request,
            Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        SellerRequestResponse response = sellerRequestService.createRequest(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/request/status")
    public ResponseEntity<SellerRequestResponse> getRequestStatus(Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        SellerRequestResponse response = sellerRequestService.getRequestByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/request")
    public ResponseEntity<SellerRequestResponse> updateRequest(
            @Valid @RequestBody SellerRequestRequest request,
            Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        SellerRequestResponse response = sellerRequestService.updateRequest(userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/request")
    public ResponseEntity<MessageResponse> cancelRequest(Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        sellerRequestService.cancelRequest(userId);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Đã hủy request thành công")
                .build());
    }

    @GetMapping("/check")
    public ResponseEntity<MessageResponse> checkSellerStatus(Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        boolean isSeller = sellerRequestService.isSeller(userId);
        boolean hasPendingRequest = sellerRequestService.hasPendingRequest(userId);
        
        String message;
        if (isSeller) {
            message = "Bạn đã là seller";
        } else if (hasPendingRequest) {
            message = "Bạn có request đang chờ duyệt";
        } else {
            message = "Bạn chưa là seller";
        }
        
        return ResponseEntity.ok(MessageResponse.builder()
                .message(message)
                .build());
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
