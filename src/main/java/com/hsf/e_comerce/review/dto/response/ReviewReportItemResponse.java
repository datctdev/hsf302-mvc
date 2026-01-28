package com.hsf.e_comerce.review.dto.response;

import com.hsf.e_comerce.review.valueobject.ReviewReportReason;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ReviewReportItemResponse {

    private UUID reportId;
    private ReviewReportReason reason;
    private String note;
    private String reporterEmail;
    private LocalDateTime createdAt;
}
