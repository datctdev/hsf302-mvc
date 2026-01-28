package com.hsf.e_comerce.review.service.impl;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.common.exception.CustomException;
import com.hsf.e_comerce.config.SecurityConfig;
import com.hsf.e_comerce.file.service.FileService;
import com.hsf.e_comerce.order.entity.Order;
import com.hsf.e_comerce.order.repository.OrderRepository;
import com.hsf.e_comerce.order.valueobject.OrderStatus;
import com.hsf.e_comerce.product.entity.Product;
import com.hsf.e_comerce.product.repository.ProductRepository;
import com.hsf.e_comerce.review.dto.request.CreateReviewRequest;
import com.hsf.e_comerce.review.dto.request.UpdateReviewRequest;
import com.hsf.e_comerce.review.dto.response.ReviewResponse;
import com.hsf.e_comerce.review.entity.Review;
import com.hsf.e_comerce.review.entity.ReviewImage;
import com.hsf.e_comerce.review.entity.SellerReviewReply;
import com.hsf.e_comerce.review.repository.ReviewImageRepository;
import com.hsf.e_comerce.review.repository.ReviewRepository;
import com.hsf.e_comerce.review.repository.SellerReviewReplyRepository;
import com.hsf.e_comerce.review.service.ReviewService;
import com.hsf.e_comerce.review.valueobject.ReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final FileService fileService;
    private final SellerReviewReplyRepository sellerReviewReplyRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> getReviewIdByUserAndProductAndSubOrder(UUID userId, UUID productId, UUID subOrderId) {
        return reviewRepository.findByUserIdAndProductIdAndSubOrderId(userId, productId, subOrderId)
                .map(Review::getId);
    }

    // --- 1. TẠO ĐÁNH GIÁ ---
    @Override
    @Transactional
    public ReviewResponse createReview(User user, UUID productId, CreateReviewRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException("Sản phẩm không tồn tại"));

        if (request.getSubOrderId() == null) {
            throw new CustomException("Vui lòng chọn đơn hàng đã mua để đánh giá.");
        }

        Order order = orderRepository.findById(request.getSubOrderId())
                .orElseThrow(() -> new CustomException("Đơn hàng không tồn tại"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new CustomException("Bạn không có quyền đánh giá đơn hàng này.");
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new CustomException("Đơn hàng chưa giao thành công.");
        }

        boolean hasProduct = order.getItems().stream()
                .anyMatch(item -> item.getProduct().getId().equals(productId));
        if (!hasProduct) {
            throw new CustomException("Sản phẩm này không có trong đơn hàng.");
        }

        // --- LOGIC: XỬ LÝ REVIEW CŨ HOẶC ĐÃ XÓA ---
        Optional<Review> existingReviewOpt = reviewRepository.findByUserIdAndProductIdAndSubOrderId(
                user.getId(), productId, request.getSubOrderId());

        Review review;
        if (existingReviewOpt.isPresent()) {
            Review existingReview = existingReviewOpt.get();
            // Nếu đã có và đang ACTIVE -> Báo lỗi
            if (existingReview.getStatus() == ReviewStatus.ACTIVE) {
                throw new CustomException("Bạn đã đánh giá sản phẩm này rồi.");
            }
            // Nếu đã có nhưng là DELETED -> KHÔI PHỤC (Resurrect)
            review = existingReview;
            review.setStatus(ReviewStatus.ACTIVE); // Kích hoạt lại
            review.setCreatedAt(LocalDateTime.now()); // Cập nhật lại ngày mới
        } else {
            // Chưa có -> Tạo mới hoàn toàn
            review = new Review();
            review.setUser(user);
            review.setProduct(product);
            review.setSubOrderId(request.getSubOrderId());
            review.setStatus(ReviewStatus.ACTIVE);
            review.setIsVerifiedPurchase(true);
        }

        // Cập nhật thông tin chung (cho cả tạo mới và khôi phục)
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        // Lưu trước để có ID (nếu tạo mới)
        review = reviewRepository.save(review);

        // Xử lý ảnh: Nếu khôi phục, nên xóa ảnh cũ đi hoặc ghi đè
        if (existingReviewOpt.isPresent()) {
            review.getImages().clear(); // Xóa ảnh cũ của review đã xóa
        }
        processImages(review, request.getImages());

        return ReviewResponse.fromEntity(review);
    }

    // --- 2. LẤY DANH SÁCH REVIEW ---
    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getProductReviews(UUID productId, int page, int size, Integer rating, Boolean hasImages, String sortBy, User currentUser) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if ("oldest".equals(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "createdAt");
        }
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Review> reviewPage = reviewRepository.findWithFilters(
                productId,
                ReviewStatus.ACTIVE,
                rating,
                hasImages,
                pageable
        );

        return reviewPage.map(review -> {
            SellerReviewReply reply =
                    sellerReviewReplyRepository
                            .findByReviewId(review.getId())
                            .orElse(null);

            return ReviewResponse.fromEntityWithSellerReply(
                    review,
                    reply,
                    currentUser
            );
        });
    }

    // --- 3. CẬP NHẬT REVIEW ---
    @Override
    @Transactional
    public ReviewResponse updateReview(User user, UUID reviewId, UpdateReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException("Đánh giá không tồn tại"));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new CustomException("Bạn không có quyền sửa đánh giá này");
        }

        if (review.getCreatedAt().plusDays(7).isBefore(LocalDateTime.now())) {
            throw new CustomException("Đã quá thời hạn 7 ngày để chỉnh sửa.");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());
        processImages(review, request.getNewImages());

        review = reviewRepository.save(review);
        return ReviewResponse.fromEntity(review);
    }

    // --- 4. XÓA REVIEW ---
    @Override
    @Transactional
    public void deleteReview(User user, UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException("Đánh giá không tồn tại"));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new CustomException("Bạn không có quyền xóa đánh giá này");
        }

        review.setStatus(ReviewStatus.DELETED);
        reviewRepository.save(review);
    }

    @Override
    public ReviewResponse getReviewById(UUID reviewId) {
        return ReviewResponse.fromEntity(reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException("Review not found")));
    }

    @Override
    public Double getAverageRating(UUID productId) {
        return reviewRepository.getAverageRating(productId);
    }

    @Override
    public long getTotalReviews(UUID productId) {
        return reviewRepository.countByProductId(productId);
    }

    private void processImages(Review review, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return;

        List<ReviewImage> images = review.getImages();
        if (images == null) images = new ArrayList<>();

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                try {
                    String url = fileService.getFileUrl(fileService.uploadFile(file, "reviews"));
                    images.add(ReviewImage.builder()
                            .review(review)
                            .imageUrl(url)
                            .displayOrder(images.size())
                            .build());
                } catch (Exception e) {
                    System.err.println("Upload failed: " + e.getMessage());
                }
            }
        }
        review.setImages(images);
    }
}