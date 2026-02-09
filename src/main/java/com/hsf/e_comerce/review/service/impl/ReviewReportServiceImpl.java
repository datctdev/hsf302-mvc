package com.hsf.e_comerce.review.service.impl;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.review.dto.request.ReportReviewRequest;
import com.hsf.e_comerce.review.dto.request.UpdateReportReviewRequest;
import com.hsf.e_comerce.review.dto.response.ReportedReviewResponse;
import com.hsf.e_comerce.review.entity.Review;
import com.hsf.e_comerce.review.entity.ReviewReport;
import com.hsf.e_comerce.review.repository.ReviewReportRepository;
import com.hsf.e_comerce.review.repository.ReviewRepository;
import com.hsf.e_comerce.review.service.ReviewReportService;
import com.hsf.e_comerce.review.valueobject.ReviewReportReason;
import com.hsf.e_comerce.review.valueobject.ReviewReportStatus;
import com.hsf.e_comerce.review.valueobject.ReviewStatus;
import lombok.RequiredArgsConstructor;
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
            throw new IllegalArgumentException("Vui lòng nhập lý do cụ thể");
        }

        // --- KIỂM TRA TRÙNG LẶP ---
        // Bất kể trạng thái báo cáo cũ là gì (PENDING, REVIEWED, REJECTED)
        // Nếu đã tồn tại trong DB thì chặn luôn.
        if (reportRepository.existsByReviewIdAndReporterId(reviewId, reporter.getId())) {
            throw new IllegalStateException("Bạn đã báo cáo đánh giá này rồi. Vui lòng chờ Admin xử lý.");
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

        // Mặc định khi tạo mới là PENDING
        report.setStatus(ReviewReportStatus.PENDING);

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

        // 1. Mark report reviewed
        reportRepository.updateStatusByReviewId(
                reviewId,
                ReviewReportStatus.REVIEWED
        );

        review.setStatus(ReviewStatus.HIDDEN);
        reviewRepository.save(review);

        // 2. Update user violation
        User reviewOwner = review.getUser();
        reviewOwner.setReviewViolationCount(
                reviewOwner.getReviewViolationCount() + 1
        );

        // 3. Nếu > 2 → ban 3 tháng
        if (reviewOwner.getReviewViolationCount() >= 2) {
            reviewOwner.setReviewBannedUntil(
                    LocalDateTime.now().plusMonths(3)
            );
            reviewOwner.setReviewViolationCount(0); // reset
        }
    }


    @Override
    @Transactional
    public void ignoreReview(UUID reviewId) {

        reportRepository.updateStatusByReviewId(
                reviewId,
                ReviewReportStatus.REJECTED
        );

    }

}

