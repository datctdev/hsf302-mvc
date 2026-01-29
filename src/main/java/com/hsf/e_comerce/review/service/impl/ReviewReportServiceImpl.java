package com.hsf.e_comerce.review.service.impl;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.review.dto.request.ReportReviewRequest;
import com.hsf.e_comerce.review.dto.request.UpdateReportReviewRequest;
import com.hsf.e_comerce.review.dto.response.ReportedReviewResponse;
import com.hsf.e_comerce.review.dto.response.ReviewReportItemResponse;
import com.hsf.e_comerce.review.entity.Review;
import com.hsf.e_comerce.review.entity.ReviewReport;
import com.hsf.e_comerce.review.repository.ReviewReportRepository;
import com.hsf.e_comerce.review.repository.ReviewRepository;
import com.hsf.e_comerce.review.service.ReviewReportService;
import com.hsf.e_comerce.review.valueobject.ReviewReportReason;
import com.hsf.e_comerce.review.valueobject.ReviewReportStatus;
import com.hsf.e_comerce.review.valueobject.ReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

        if (reportRepository.existsByReviewIdAndReporterId(reviewId, reporter.getId())) {
            throw new RuntimeException("Bạn đã báo cáo đánh giá này");
        }

        if (review.getStatus() == ReviewStatus.DISABLED) {
            throw new RuntimeException("Đánh giá này đã bị vô hiệu hóa và không thể báo cáo");
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

        review.setFlagged(true);

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

    @Override
    @Transactional
    public void updateReport(
            UUID reviewId,
            User reporter,
            UpdateReportReviewRequest request
    ) {
        ReviewReport report = reportRepository
                .findByReviewIdAndReporterId(reviewId, reporter.getId())
                .orElseThrow(() -> new RuntimeException("Bạn chưa báo cáo đánh giá này"));

        if (report.getStatus() != ReviewReportStatus.PENDING) {
            throw new RuntimeException("Báo cáo đã được xử lý, không thể chỉnh sửa");
        }

        if (request.getReason() == ReviewReportReason.OTHER &&
                (request.getNote() == null || request.getNote().isBlank())) {
            throw new RuntimeException("Vui lòng nhập lý do cụ thể");
        }

        report.setReason(request.getReason());
        report.setNote(request.getNote());

        reportRepository.save(report);
    }

    @Override
    @Transactional
    public void hideReview(UUID reviewId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review không tồn tại"));

        // 1. Đánh dấu report đã xử lý
        reportRepository.updateStatusByReviewId(
                reviewId,
                ReviewReportStatus.REVIEWED
        );

        review.setFlagged(true);

        // 3. Tính số lần bị flag trong 6 tháng
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);

        UUID reviewOwnerId = review.getUser().getId();

        long flagsIn6Months =
                reportRepository.countReviewedReportsByUserInPeriod(
                        reviewOwnerId,
                        sixMonthsAgo
                );

        // 4. Áp rule
        if (flagsIn6Months >= 5) {
            review.setStatus(ReviewStatus.DISABLED);
        } else {
            review.setStatus(ReviewStatus.HIDDEN);
        }
        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void ignoreReview(UUID reviewId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review không tồn tại"));

        // Từ chối tất cả report pending
        reportRepository.updateStatusByReviewId(
                reviewId,
                ReviewReportStatus.REJECTED
        );

        // Không động gì tới review
    }
}

