package com.hsf.e_comerce.review.service;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.review.dto.request.ReportReviewRequest;
import com.hsf.e_comerce.review.dto.request.UpdateReportReviewRequest;
import com.hsf.e_comerce.review.dto.response.ReportedReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

public interface ReviewReportService {

    void reportReview(
            UUID reviewId,
            User reporter,
            String ip,
            ReportReviewRequest request
    );

    long countPendingReportedReviews();

    List<ReportedReviewResponse> getPendingReportedReviews();

    void updateReport(
            UUID reviewId,
            User reporter,
            UpdateReportReviewRequest request
    );

    void hideReview(UUID reviewId);
    void ignoreReview(UUID reviewId);

}
