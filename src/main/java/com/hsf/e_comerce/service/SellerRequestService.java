package com.hsf.e_comerce.service;

import com.hsf.e_comerce.dto.request.RejectSellerRequestRequest;
import com.hsf.e_comerce.dto.request.SellerRequestRequest;
import com.hsf.e_comerce.dto.response.SellerRequestResponse;
import com.hsf.e_comerce.valueobject.SellerRequestStatus;

import java.util.List;
import java.util.UUID;

public interface SellerRequestService {
    
    SellerRequestResponse createRequest(UUID userId, SellerRequestRequest request);
    
    SellerRequestResponse getRequestByUserId(UUID userId);
    
    SellerRequestResponse getRequestById(UUID requestId);
    
    List<SellerRequestResponse> getAllRequests();
    
    List<SellerRequestResponse> getRequestsByStatus(String status);
    
    SellerRequestResponse approveRequest(UUID requestId, UUID adminId);
    
    SellerRequestResponse rejectRequest(UUID requestId, UUID adminId, RejectSellerRequestRequest request);
    
    SellerRequestResponse updateRequest(UUID userId, SellerRequestRequest request);
    
    void cancelRequest(UUID userId);
    
    boolean hasPendingRequest(UUID userId);
    
    boolean isSeller(UUID userId);
}
