package com.hsf.e_comerce.review.service;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.review.dto.request.CreateReviewRequest;
import com.hsf.e_comerce.review.dto.request.UpdateReviewRequest;
import com.hsf.e_comerce.review.dto.response.ReviewResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ReviewService {

    // 1. Tạo đánh giá mới (Cần User, ProductId và DTO)
    ReviewResponse createReview(User user, UUID productId, CreateReviewRequest request);

    // 2. Lấy danh sách đánh giá của sản phẩm (Phân trang, lọc theo sao)
    Page<ReviewResponse> getProductReviews(UUID productId, int page, int size, Integer rating, Boolean hasImages, String sortBy);

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
}