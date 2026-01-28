package com.hsf.e_comerce.review.dto.response;

import com.hsf.e_comerce.review.valueobject.ReviewReportReason;
import com.hsf.e_comerce.review.valueobject.ReviewReportStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ReportedReviewResponse {

    private UUID reviewId;
    private String reviewContent;
    private String reviewOwnerEmail;

    private long reportCount;
    private LocalDateTime lastReportedAt;

    private List<ReviewReportItemResponse> reports;
}


