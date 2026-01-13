package com.hsf.e_comerce.seller.controller;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.seller.dto.request.RejectSellerRequestRequest;
import com.hsf.e_comerce.seller.dto.response.SellerRequestResponse;
import com.hsf.e_comerce.seller.service.SellerRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/seller-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSellerController {

    private final SellerRequestService sellerRequestService;

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
            @CurrentUser User admin) {
        SellerRequestResponse response = sellerRequestService.approveRequest(id, admin.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<SellerRequestResponse> rejectRequest(
            @PathVariable UUID id,
            @Valid @RequestBody RejectSellerRequestRequest request,
            @CurrentUser User admin) {
        SellerRequestResponse response = sellerRequestService.rejectRequest(id, admin.getId(), request);
        return ResponseEntity.ok(response);
    }
}
