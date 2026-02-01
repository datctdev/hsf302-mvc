package com.hsf.e_comerce.review.service;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.review.dto.response.ReviewResponse;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface SellerReviewReplyService {
    UUID replyToReview(User seller, UUID reviewId, String reply);

    UUID updateReply(User seller, UUID reviewId, String reply);

}
