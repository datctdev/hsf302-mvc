package com.hsf.e_comerce.controller;

import com.hsf.e_comerce.dto.request.RejectSellerRequestRequest;
import com.hsf.e_comerce.dto.response.MessageResponse;
import com.hsf.e_comerce.dto.response.SellerRequestResponse;
import com.hsf.e_comerce.exception.CustomException;
import com.hsf.e_comerce.service.SellerRequestService;
import com.hsf.e_comerce.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/seller-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSellerController {

    private final SellerRequestService sellerRequestService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<SellerRequestResponse>> getAllRequests(
            @RequestParam(required = false) String status) {
        List<SellerRequestResponse> requests;
        if (status != null && !status.isEmpty()) {
            requests = sellerRequestService.getRequestsByStatus(status);
        } else {
            requests = sellerRequestService.getAllRequests();
        }
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SellerRequestResponse> getRequestById(@PathVariable UUID id) {
        SellerRequestResponse response = sellerRequestService.getRequestById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<SellerRequestResponse> approveRequest(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID adminId = getUserIdFromAuthentication(authentication);
        SellerRequestResponse response = sellerRequestService.approveRequest(id, adminId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<SellerRequestResponse> rejectRequest(
            @PathVariable UUID id,
            @Valid @RequestBody RejectSellerRequestRequest request,
            Authentication authentication) {
        UUID adminId = getUserIdFromAuthentication(authentication);
        SellerRequestResponse response = sellerRequestService.rejectRequest(id, adminId, request);
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
