package com.hsf.e_comerce.review.service;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.review.dto.request.CreateReviewRequest;
import com.hsf.e_comerce.review.dto.request.UpdateReviewRequest;
import com.hsf.e_comerce.review.dto.response.ReviewPermissionResponse;
import com.hsf.e_comerce.review.dto.response.ReviewResponse;
import com.hsf.e_comerce.review.dto.response.ReviewSummaryDTO;
import com.hsf.e_comerce.review.entity.Review;
import com.hsf.e_comerce.review.valueobject.ReviewStatus;
import org.springframework.data.domain.Page;

import java.util.Optional;
import java.util.UUID;

public interface ReviewService {

    Optional<UUID> getReviewIdByUserAndProductAndSubOrder(UUID userId, UUID productId, UUID subOrderId);

    // 1. Tạo đánh giá mới (Cần User, ProductId và DTO)
    ReviewResponse createReview(User user, UUID productId, CreateReviewRequest request);

    // 2. Lấy danh sách đánh giá của sản phẩm (Phân trang, lọc theo sao)
    Page<ReviewResponse> getProductReviews(UUID productId, int page, int size, Integer rating, Boolean hasImages, String sortBy, User currentUser);

    // 3. Cập nhật đánh giá (User sửa bài của mình)
    ReviewResponse updateReview(User user, UUID reviewId, UpdateReviewRequest request);

    // 4. Xóa đánh giá
    void deleteReview(User user, UUID reviewId);

    // 5. Lấy chi tiết 1 review (Dùng cho trang Edit)
    ReviewResponse getReviewById(UUID reviewId);

    // 6. Tính điểm trung bình (VD: 4.5 sao)
    Double getAverageRating(UUID productId);

    // 7. Đếm tổng số review (VD: 100 đánh giá)
    long getTotalReviews(UUID productId);

    // 8. Hàm mới cho Admin
    Page<Review> getAllReviewsForAdmin(String keyword, Integer rating, ReviewStatus status, UUID shopId, int page, int size);

    // 9. Hàm ẩn/hiện review
    void toggleReviewVisibility(UUID reviewId);

    // 10. Kiểm tra quyền đánh giá (Có thể đánh giá hay không)
    ReviewPermissionResponse checkReviewPermission(User user);

    // 11. Lấy tổng quan đánh giá (số lượng từng sao, có ảnh, có bình luận)
    ReviewSummaryDTO getReviewSummary(UUID productId);
}