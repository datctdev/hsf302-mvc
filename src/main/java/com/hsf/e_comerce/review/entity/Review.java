package com.hsf.e_comerce.review.entity;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.product.entity.Product;
import com.hsf.e_comerce.review.valueobject.ReviewStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reviews",
        indexes = {
                @Index(name = "idx_reviews_product", columnList = "product_id"),
                @Index(name = "idx_reviews_user", columnList = "user_id")
        },
        // Ràng buộc: 1 User - 1 Sản phẩm - 1 Đơn hàng chỉ có 1 Review (Tránh spam)
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id", "sub_order_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Lưu ID đơn hàng để xác thực "Đã mua hàng" (Verified Purchase)
    @Column(name = "sub_order_id", columnDefinition = "uuid")
    private UUID subOrderId;

    @Column(name = "rating", nullable = false)
    private Integer rating; // 1-5 sao

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "seller_reply", columnDefinition = "TEXT")
    private String sellerReply;

    @Column(name = "is_verified_purchase", nullable = false)
    @Builder.Default
    private Boolean isVerifiedPurchase = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.ACTIVE;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ReviewImage> images = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}