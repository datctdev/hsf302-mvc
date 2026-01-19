package com.hsf.e_comerce.seller.service.impl;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.auth.repository.UserRepository;
import com.hsf.e_comerce.auth.service.UserService;
import com.hsf.e_comerce.common.exception.CustomException;
import com.hsf.e_comerce.shop.entity.Shop;
import com.hsf.e_comerce.shop.repository.ShopRepository;
import com.hsf.e_comerce.shop.valueobject.ShopStatus;
import com.hsf.e_comerce.seller.dto.request.RejectSellerRequestRequest;
import com.hsf.e_comerce.seller.dto.request.SellerRequestRequest;
import com.hsf.e_comerce.seller.dto.response.SellerRequestResponse;
import com.hsf.e_comerce.seller.entity.SellerRequest;
import com.hsf.e_comerce.seller.repository.SellerRequestRepository;
import com.hsf.e_comerce.seller.service.SellerRequestService;
import com.hsf.e_comerce.seller.valueobject.SellerRequestStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SellerRequestServiceImpl implements SellerRequestService {

    private final SellerRequestRepository sellerRequestRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ShopRepository shopRepository;

    @Override
    @Transactional
    public SellerRequestResponse createRequest(UUID userId, SellerRequestRequest request) {
        // Kiểm tra user đã là seller chưa
        if (isSeller(userId)) {
            throw new CustomException("Bạn đã là seller. Không thể tạo request mới.");
        }

        // Kiểm tra user đã có shop chưa
        if (shopRepository.existsByUserId(userId)) {
            throw new CustomException("Bạn đã có shop. Không thể tạo request mới.");
        }

        // Kiểm tra có request pending không
        if (hasPendingRequest(userId)) {
            throw new CustomException("Bạn đã có request đang chờ duyệt. Vui lòng đợi admin xử lý.");
        }

        // Kiểm tra tên shop đã tồn tại chưa
        if (shopRepository.existsByName(request.getShopName())) {
            throw new CustomException("Tên shop đã tồn tại. Vui lòng chọn tên khác.");
        }

        User user = userService.findById(userId);

        SellerRequest sellerRequest = new SellerRequest();
        sellerRequest.setUser(user);
        sellerRequest.setShopName(request.getShopName());
        sellerRequest.setShopDescription(request.getShopDescription());
        sellerRequest.setShopPhone(request.getShopPhone());
        sellerRequest.setShopAddress(request.getShopAddress());
        sellerRequest.setLogoUrl(request.getLogoUrl());
        sellerRequest.setCoverImageUrl(request.getCoverImageUrl());
        sellerRequest.setStatus(SellerRequestStatus.PENDING);

        sellerRequest = sellerRequestRepository.save(sellerRequest);

        return mapToResponse(sellerRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public SellerRequestResponse getRequestByUserId(UUID userId) {
        SellerRequest sellerRequest = sellerRequestRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException("Không tìm thấy request của bạn."));
        return mapToResponse(sellerRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public SellerRequestResponse getRequestById(UUID requestId) {
        SellerRequest sellerRequest = sellerRequestRepository.findById(requestId)
                .orElseThrow(() -> new CustomException("Không tìm thấy request."));
        return mapToResponse(sellerRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SellerRequestResponse> getAllRequests() {
        return sellerRequestRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SellerRequestResponse> getRequestsByStatus(String status) {
        SellerRequestStatus statusEnum = SellerRequestStatus.fromCode(status);
        return sellerRequestRepository.findByStatusOrderByCreatedAtDesc(statusEnum).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SellerRequestResponse approveRequest(UUID requestId, UUID adminId) {
        SellerRequest sellerRequest = sellerRequestRepository.findById(requestId)
                .orElseThrow(() -> new CustomException("Không tìm thấy request."));

        if (sellerRequest.getStatus() != SellerRequestStatus.PENDING) {
            throw new CustomException("Request này đã được xử lý rồi.");
        }

        User user = sellerRequest.getUser();
        UUID userId = user.getId();

        // Kiểm tra user đã có shop chưa (tránh trường hợp đã approve trước đó)
        if (shopRepository.existsByUserId(userId)) {
            throw new CustomException("User này đã có shop rồi.");
        }

        // Kiểm tra tên shop đã tồn tại chưa
        if (shopRepository.existsByName(sellerRequest.getShopName())) {
            throw new CustomException("Tên shop đã tồn tại. Vui lòng từ chối request này.");
        }

        User admin = userService.findById(adminId);

        // Gán role SELLER cho user
        List<String> userRoles = userService.getUserRoles(userId);
        if (!userRoles.contains("ROLE_SELLER")) {
            userService.assignRoleToUser(user, "ROLE_SELLER");
        }

        // Tạo shop (không yêu cầu địa chỉ - có thể cập nhật sau)
        Shop shop = new Shop();
        shop.setUser(user);
        shop.setName(sellerRequest.getShopName());
        shop.setDescription(sellerRequest.getShopDescription());
        shop.setPhoneNumber(sellerRequest.getShopPhone());
        shop.setAddress(sellerRequest.getShopAddress()); // Có thể null
        shop.setLogoUrl(sellerRequest.getLogoUrl());
        shop.setCoverImageUrl(sellerRequest.getCoverImageUrl());
        shop.setStatus(ShopStatus.ACTIVE);
        // districtId và wardCode sẽ được cập nhật sau khi seller cập nhật địa chỉ
        shopRepository.save(shop);

        // Cập nhật request
        sellerRequest.setStatus(SellerRequestStatus.APPROVED);
        sellerRequest.setReviewedBy(admin);
        sellerRequest.setReviewedAt(LocalDateTime.now());
        sellerRequest = sellerRequestRepository.save(sellerRequest);

        return mapToResponse(sellerRequest);
    }

    @Override
    @Transactional
    public SellerRequestResponse rejectRequest(UUID requestId, UUID adminId, RejectSellerRequestRequest request) {
        SellerRequest sellerRequest = sellerRequestRepository.findById(requestId)
                .orElseThrow(() -> new CustomException("Không tìm thấy request."));

        if (sellerRequest.getStatus() != SellerRequestStatus.PENDING) {
            throw new CustomException("Request này đã được xử lý rồi.");
        }

        User admin = userService.findById(adminId);

        sellerRequest.setStatus(SellerRequestStatus.REJECTED);
        sellerRequest.setRejectionReason(request.getRejectionReason());
        sellerRequest.setReviewedBy(admin);
        sellerRequest.setReviewedAt(LocalDateTime.now());
        sellerRequest = sellerRequestRepository.save(sellerRequest);

        return mapToResponse(sellerRequest);
    }

    @Override
    @Transactional
    public SellerRequestResponse updateRequest(UUID userId, SellerRequestRequest request) {
        SellerRequest sellerRequest = sellerRequestRepository.findPendingRequestByUserId(userId)
                .orElseThrow(() -> new CustomException("Không tìm thấy request đang chờ duyệt của bạn."));

        if (sellerRequest.getStatus() != SellerRequestStatus.PENDING && 
            sellerRequest.getStatus() != SellerRequestStatus.REJECTED) {
            throw new CustomException("Chỉ có thể cập nhật request đang chờ duyệt hoặc bị từ chối.");
        }

        // Kiểm tra tên shop mới có trùng không (nếu đổi tên)
        if (!sellerRequest.getShopName().equals(request.getShopName()) 
                && shopRepository.existsByName(request.getShopName())) {
            throw new CustomException("Tên shop đã tồn tại. Vui lòng chọn tên khác.");
        }

        sellerRequest.setShopName(request.getShopName());
        sellerRequest.setShopDescription(request.getShopDescription());
        sellerRequest.setShopPhone(request.getShopPhone());
        sellerRequest.setShopAddress(request.getShopAddress());
        sellerRequest.setLogoUrl(request.getLogoUrl());
        sellerRequest.setCoverImageUrl(request.getCoverImageUrl());
        
        // Nếu đang REJECTED, chuyển về PENDING khi update
        if (sellerRequest.getStatus() == SellerRequestStatus.REJECTED) {
            sellerRequest.setStatus(SellerRequestStatus.PENDING);
            sellerRequest.setRejectionReason(null);
            sellerRequest.setReviewedBy(null);
            sellerRequest.setReviewedAt(null);
        }

        sellerRequest = sellerRequestRepository.save(sellerRequest);

        return mapToResponse(sellerRequest);
    }

    @Override
    @Transactional
    public void cancelRequest(UUID userId) {
        SellerRequest sellerRequest = sellerRequestRepository.findPendingRequestByUserId(userId)
                .orElseThrow(() -> new CustomException("Không tìm thấy request đang chờ duyệt của bạn."));

        sellerRequestRepository.delete(sellerRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPendingRequest(UUID userId) {
        return sellerRequestRepository.existsByUserIdAndStatus(userId, SellerRequestStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSeller(UUID userId) {
        List<String> roles = userService.getUserRoles(userId);
        return roles.contains("ROLE_SELLER");
    }

    private SellerRequestResponse mapToResponse(SellerRequest sellerRequest) {
        return SellerRequestResponse.builder()
                .id(sellerRequest.getId())
                .userId(sellerRequest.getUser().getId())
                .shopName(sellerRequest.getShopName())
                .shopDescription(sellerRequest.getShopDescription())
                .shopPhone(sellerRequest.getShopPhone())
                .shopAddress(sellerRequest.getShopAddress())
                .logoUrl(sellerRequest.getLogoUrl())
                .coverImageUrl(sellerRequest.getCoverImageUrl())
                .status(sellerRequest.getStatus().getCode())
                .rejectionReason(sellerRequest.getRejectionReason())
                .reviewedBy(sellerRequest.getReviewedBy() != null ? sellerRequest.getReviewedBy().getId() : null)
                .reviewedAt(sellerRequest.getReviewedAt())
                .createdAt(sellerRequest.getCreatedAt())
                .updatedAt(sellerRequest.getUpdatedAt())
                .build();
    }
}
