package com.hsf.e_comerce.review.dto.response;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.review.entity.Review;
import com.hsf.e_comerce.review.entity.ReviewImage;
import com.hsf.e_comerce.review.entity.SellerReviewReply;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
public class ReviewResponse {
    private UUID id;
    private UUID userId;
    private UUID productId;
    private String userFullName;
    private String userAvatarUrl;
    private Integer rating;
    private String comment;
    private SellerReplyResponse sellerReply;
    private Boolean sellerCanReply;
    private Boolean isVerifiedPurchase;
    private List<String> imageUrls;
    private LocalDateTime createdAt;

    public static ReviewResponse fromEntity(Review review) {
        // 1. Logic ẩn tên
        String originalName = review.getUser().getFullName();
        if (originalName == null || originalName.isEmpty()) {
            originalName = review.getUser().getEmail(); // Fallback nếu không có tên
        }

        String maskedName = originalName;
        if (originalName.length() > 2) {
            // Lấy ký tự đầu và cuối, ở giữa thay bằng ***
            maskedName = originalName.charAt(0) + "****" + originalName.charAt(originalName.length() - 1);
        }

        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .productId(review.getProduct().getId())
                .userFullName(maskedName) // Trả về tên đã ẩn
                .userAvatarUrl(review.getUser().getAvatarUrl()) // Server trả về link ảnh gốc (hoặc null)
                .rating(review.getRating())
                .comment(review.getComment())
                .isVerifiedPurchase(review.getIsVerifiedPurchase())
                .imageUrls(review.getImages().stream()
                        .map(ReviewImage::getImageUrl)
                        .collect(Collectors.toList()))
                .createdAt(review.getCreatedAt())
                .build();
    }

    public static ReviewResponse fromEntityWithSellerReply(
            Review review,
            SellerReviewReply replyEntity,
            User currentUser
    ) {
        ReviewResponse res = fromEntity(review);

        boolean canReply = false;

        if (currentUser != null
                && currentUser.getRole() != null
                && "ROLE_SELLER".equals(currentUser.getRole().getName())
                && review.getProduct()
                .getShop()
                .getUser()
                .getId()
                .equals(currentUser.getId())) {

            if (replyEntity == null) {
                canReply = true;
            } else {
                canReply = replyEntity.getRepliedAt()
                        .isAfter(LocalDateTime.now().minusDays(7));
            }
        }

        res.setSellerCanReply(canReply);

        if (replyEntity != null) {
            boolean editable = canReply;

            res.setSellerReply(
                    SellerReplyResponse.builder()
                            .reply(replyEntity.getReply())
                            .repliedAt(replyEntity.getRepliedAt())
                            .editable(editable)
                            .build()
            );
        }

        return res;
    }

}