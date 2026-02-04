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
import com.hsf.e_comerce.review.dto.response.ReviewPermissionResponse;
import com.hsf.e_comerce.review.dto.response.ReviewReportItemResponse;
import com.hsf.e_comerce.review.dto.response.ReviewResponse;
import com.hsf.e_comerce.review.dto.response.ReviewSummaryDTO;
import com.hsf.e_comerce.review.entity.Review;
import com.hsf.e_comerce.review.entity.ReviewImage;
import com.hsf.e_comerce.review.entity.ReviewReport;
import com.hsf.e_comerce.review.entity.SellerReviewReply;
import com.hsf.e_comerce.review.repository.*;
import com.hsf.e_comerce.review.service.ReviewService;
import com.hsf.e_comerce.review.valueobject.ReviewReportStatus;
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
    private final ReviewReportRepository reviewReportRepository;
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

        if (!order.isReceivedByBuyer()) {
            throw new CustomException("Vui lòng xác nhận đã nhận hàng trước khi đánh giá.");
        }

        boolean hasProduct = order.getItems().stream()
                .anyMatch(item -> item.getProduct().getId().equals(productId));
        if (!hasProduct) {
            throw new CustomException("Sản phẩm này không có trong đơn hàng.");
        }

        ReviewPermissionResponse permission = checkReviewPermission(user);

        if (!permission.isAllowed()) {
            throw new CustomException(permission.getMessage());
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

    @Override
    public ReviewPermissionResponse checkReviewPermission(User user) {

        // 1. Đang bị ban?
        if (user.getReviewBannedUntil() != null) {
            if (user.getReviewBannedUntil().isAfter(LocalDateTime.now())) {
                return new ReviewPermissionResponse(
                        false,
                        false,
                        "Bạn bị cấm đánh giá đến " +
                                user.getReviewBannedUntil()
                );
            } else {
                // Hết hạn ban → reset
                user.setReviewBannedUntil(null);
                user.setReviewViolationCount(0);
            }
        }

        // 2. Cảnh báo từ lần thứ 2
        if (user.getReviewViolationCount() >= 1) {
            return new ReviewPermissionResponse(
                    true,
                    true,
                    "Tài khoản của bạn đã nhiều lần vi phạm nội dung đánh giá. Vui lòng chú ý ngôn từ."
            );
        }

        return new ReviewPermissionResponse(true, false, null);
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

            // 2️⃣ User report (nếu đã đăng nhập)
            ReviewReportItemResponse userReport = null;

            if (currentUser != null) {
                userReport = reviewReportRepository
                        .findByReviewIdAndReporterId(
                                review.getId(),
                                currentUser.getId()
                        )
                        .map(r -> new ReviewReportItemResponse(
                                r.getId(),
                                r.getReason(),
                                r.getNote(),
                                r.getReporter().getEmail(),
                                r.getCreatedAt()
                        ))
                        .orElse(null);
            }

            // 3️⃣ Build ReviewResponse
            ReviewResponse response =
                    ReviewResponse.fromEntityWithSellerReply(
                            review,
                            reply,
                            currentUser
                    );

            response.setUserReport(userReport);

            return response;
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

        ReviewPermissionResponse permission = checkReviewPermission(user);
        if (!permission.isAllowed()) {
            throw new CustomException(
                    "Bạn đang bị cấm đánh giá nên không thể chỉnh sửa đánh giá cũ."
            );
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

    @Override
    @Transactional(readOnly = true)
    public Page<Review> getAllReviewsForAdmin(String keyword, Integer rating, ReviewStatus status, UUID shopId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return reviewRepository.findAll(
                ReviewSpecification.filterForAdmin(keyword, rating, status, shopId),
                pageable
        );
    }

    @Override
    @Transactional
    public void toggleReviewVisibility(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException("Đánh giá không tồn tại"));

        // Logic toggle: Nếu đang ACTIVE -> HIDDEN, ngược lại thì ACTIVE
        if (review.getStatus() == ReviewStatus.ACTIVE) {
            review.setStatus(ReviewStatus.HIDDEN);
        } else if (review.getStatus() == ReviewStatus.HIDDEN) {
            review.setStatus(ReviewStatus.ACTIVE);
        } else {
            // Nếu đang là DELETED hoặc DISABLED (do vi phạm nặng), có thể cho phép khôi phục về HIDDEN trước
            review.setStatus(ReviewStatus.ACTIVE);
        }
        reviewRepository.save(review);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewSummaryDTO getReviewSummary(UUID productId) {
        long total = reviewRepository.countByProductId(productId);
        if (total == 0) {
            return new ReviewSummaryDTO(0.0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        double avg = reviewRepository.getAverageRating(productId);

        avg = Math.round(avg * 10.0) / 10.0;

        return new ReviewSummaryDTO(
                avg,
                total,
                reviewRepository.countByProductIdAndStatusAndRating(productId, ReviewStatus.ACTIVE, 5),
                reviewRepository.countByProductIdAndStatusAndRating(productId, ReviewStatus.ACTIVE, 4),
                reviewRepository.countByProductIdAndStatusAndRating(productId, ReviewStatus.ACTIVE, 3),
                reviewRepository.countByProductIdAndStatusAndRating(productId, ReviewStatus.ACTIVE, 2),
                reviewRepository.countByProductIdAndStatusAndRating(productId, ReviewStatus.ACTIVE, 1),
                reviewRepository.countWithImages(productId),
                reviewRepository.countWithComments(productId)
        );
    }

}