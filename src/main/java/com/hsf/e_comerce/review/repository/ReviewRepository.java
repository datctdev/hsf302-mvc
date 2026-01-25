package com.hsf.e_comerce.review.repository;

import com.hsf.e_comerce.review.entity.Review;
import com.hsf.e_comerce.review.valueobject.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    // Lấy list review cho trang chi tiết (Chỉ lấy ACTIVE)
    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND r.status = :status")
    Page<Review> findByProductIdAndStatus(
            @Param("productId") UUID productId,
            @Param("status") ReviewStatus status,
            Pageable pageable
    );

    // Filter theo Rating (1-5 sao)
    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND r.status = :status AND r.rating = :rating")
    Page<Review> findByProductIdAndStatusAndRating(
            @Param("productId") UUID productId,
            @Param("status") ReviewStatus status,
            @Param("rating") Integer rating,
            Pageable pageable
    );

    // Hàm tìm kiếm FULL bộ lọc (Rating + HasImages)
    @Query("SELECT r FROM Review r " +
            "WHERE r.product.id = :productId " +
            "AND r.status = :status " +
            "AND (:rating IS NULL OR r.rating = :rating) " +
            "AND (:hasImages IS NULL OR :hasImages = false OR SIZE(r.images) > 0)")
    Page<Review> findWithFilters(
            @Param("productId") UUID productId,
            @Param("status") ReviewStatus status,
            @Param("rating") Integer rating,
            @Param("hasImages") Boolean hasImages,
            Pageable pageable
    );

    // Check trùng lặp review
    boolean existsByUserIdAndProductIdAndSubOrderId(UUID userId, UUID productId, UUID subOrderId);

    // Kiểm tra xem có review nào đang ACTIVE không (Dùng cho OrderService để hiển thị nút)
    boolean existsByUserIdAndProductIdAndSubOrderIdAndStatus(
            UUID userId, UUID productId, UUID subOrderId, ReviewStatus status
    );

    // Tìm review bất kỳ (kể cả đã xóa) để xử lý logic khôi phục
    Optional<Review> findByUserIdAndProductIdAndSubOrderId(
            UUID userId, UUID productId, UUID subOrderId
    );

    // Tính điểm trung bình
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.status = 'ACTIVE'")
    Double getAverageRating(@Param("productId") UUID productId);

    // Đếm số lượng review
    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.status = 'ACTIVE'")
    long countByProductId(@Param("productId") UUID productId);

}