package com.hsf.e_comerce.review.service.impl;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.common.exception.CustomException;
import com.hsf.e_comerce.review.dto.response.ReviewResponse;
import com.hsf.e_comerce.review.entity.Review;
import com.hsf.e_comerce.review.entity.SellerReviewReply;
import com.hsf.e_comerce.review.repository.ReviewRepository;
import com.hsf.e_comerce.review.repository.SellerReviewReplyRepository;
import com.hsf.e_comerce.review.service.SellerReviewReplyService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerReviewReplyServiceImpl
        implements SellerReviewReplyService {

    private final ReviewRepository reviewRepository;
    private final SellerReviewReplyRepository replyRepository;

    @Override
    @Transactional
    public UUID replyToReview(User seller, UUID reviewId, String reply) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException("Review không tồn tại"));

        // 1. Review phải thuộc shop của seller
        if (!review.getProduct().getShop().getUser().getId()
                .equals(seller.getId())) {
            throw new CustomException("Bạn không có quyền phản hồi review này");
        }

        // 2. Mỗi review chỉ được reply 1 lần
        if (replyRepository.existsByReviewId(reviewId)) {
            throw new CustomException("Review này đã được phản hồi");
        }

        // 3. Validate nội dung
        if (reply == null || reply.isBlank() || reply.length() > 1000) {
            throw new CustomException("Nội dung phản hồi không hợp lệ");
        }

        SellerReviewReply entity = SellerReviewReply.builder()
                .review(review)
                .reply(reply.trim())
                .build();

        replyRepository.save(entity);
        return review.getProduct().getId();

        // TODO: gửi notification cho buyer (sau)
    }

    @Override
    @Transactional
    public void updateReply(User seller, UUID reviewId, String reply) {

        SellerReviewReply entity = replyRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new CustomException("Chưa có phản hồi để sửa"));

        // 1. Check owner shop
        if (!entity.getReview().getProduct().getShop().getUser().getId()
                .equals(seller.getId())) {
            throw new CustomException("Bạn không có quyền sửa phản hồi này");
        }

        // 2. Check 7 ngày
        if (entity.getRepliedAt()
                .plusDays(7)
                .isBefore(LocalDateTime.now())) {
            throw new CustomException("Đã quá 7 ngày, không thể chỉnh sửa");
        }

        entity.setReply(reply.trim());
        replyRepository.save(entity);
    }

}

