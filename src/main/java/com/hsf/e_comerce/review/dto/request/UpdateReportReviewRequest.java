package com.hsf.e_comerce.review.dto.request;

import com.hsf.e_comerce.review.valueobject.ReviewReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateReportReviewRequest {

    @NotNull
    private ReviewReportReason reason;

    @Size(max = 500)
    private String note;
}