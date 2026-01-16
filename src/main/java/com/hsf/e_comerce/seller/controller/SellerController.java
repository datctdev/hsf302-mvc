package com.hsf.e_comerce.seller.controller;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.common.dto.response.MessageResponse;
import com.hsf.e_comerce.seller.dto.request.SellerRequestRequest;
import com.hsf.e_comerce.seller.dto.response.SellerRequestResponse;
import com.hsf.e_comerce.seller.service.SellerRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class SellerController {

    private final SellerRequestService sellerRequestService;

    @PostMapping("/request")
    public ResponseEntity<SellerRequestResponse> createRequest(
            @CurrentUser User user,
            @Valid @RequestBody SellerRequestRequest request) {
        SellerRequestResponse response = sellerRequestService.createRequest(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/request/status")
    public ResponseEntity<SellerRequestResponse> getRequestStatus(@CurrentUser User user) {
        SellerRequestResponse response = sellerRequestService.getRequestByUserId(user.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/request")
    public ResponseEntity<SellerRequestResponse> updateRequest(
            @CurrentUser User user,
            @Valid @RequestBody SellerRequestRequest request) {
        SellerRequestResponse response = sellerRequestService.updateRequest(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/request")
    public ResponseEntity<MessageResponse> cancelRequest(@CurrentUser User user) {
        sellerRequestService.cancelRequest(user.getId());
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Đã hủy request thành công")
                .build());
    }

    @GetMapping("/check")
    public ResponseEntity<MessageResponse> checkSellerStatus(@CurrentUser User user) {
        boolean isSeller = sellerRequestService.isSeller(user.getId());
        boolean hasPendingRequest = sellerRequestService.hasPendingRequest(user.getId());
        
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
}
