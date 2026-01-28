package com.hsf.e_comerce.review.service.impl;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.review.dto.request.ReportReviewRequest;
import com.hsf.e_comerce.review.dto.response.ReportedReviewResponse;
import com.hsf.e_comerce.review.entity.Review;
import com.hsf.e_comerce.review.entity.ReviewReport;
import com.hsf.e_comerce.review.repository.ReviewReportRepository;
import com.hsf.e_comerce.review.repository.ReviewRepository;
import com.hsf.e_comerce.review.service.ReviewReportService;
import com.hsf.e_comerce.review.valueobject.ReviewReportReason;
import com.hsf.e_comerce.review.valueobject.ReviewReportStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewReportServiceImpl implements ReviewReportService {

    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reportRepository;

    @Override
    @Transactional
    public void reportReview(
            UUID reviewId,
            User reporter,
            String ip,
            ReportReviewRequest request
    ) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review không tồn tại"));

        if (request.getReason() == ReviewReportReason.OTHER &&
                (request.getNote() == null || request.getNote().isBlank())) {
            throw new RuntimeException("Vui lòng nhập lý do cụ thể");
        }

        ReviewReport report = new ReviewReport();
        report.setReview(review);
        report.setReporter(reporter);
        report.setReporterIp(ip);
        report.setReason(request.getReason());
        report.setNote(request.getNote());

        reportRepository.save(report);

        long count = reportRepository.countByReviewId(reviewId);
        review.setReportCount((int) count);

        if (count >= 3) {
            review.setFlagged(true);
        }

        reviewRepository.save(review);
    }

    @Override
    public long countPendingReportedReviews() {
        return reportRepository.countDistinctReviewByStatus(
                ReviewReportStatus.PENDING
        );
    }

    @Override
    public List<ReportedReviewResponse> getPendingReportedReviews() {

        List<Object[]> rows =
                reportRepository.findReportedReviewsGrouped(
                        ReviewReportStatus.PENDING
                );

        return rows.stream().map(row -> {
            UUID reviewId = (UUID) row[0];

            return new ReportedReviewResponse(
                    reviewId,
                    (String) row[1],
                    (String) row[2],
                    (Long) row[3],
                    (LocalDateTime) row[4],
                    reportRepository.findReportsByReviewId(
                            reviewId,
                            ReviewReportStatus.PENDING
                    )
            );
        }).toList();
    }

}

