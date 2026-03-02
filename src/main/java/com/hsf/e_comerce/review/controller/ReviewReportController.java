package com.hsf.e_comerce.review.controller;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.review.dto.request.ReportReviewRequest;
import com.hsf.e_comerce.review.dto.request.UpdateReportReviewRequest;
import com.hsf.e_comerce.review.service.ReviewReportService;
import com.hsf.e_comerce.review.valueobject.ReviewReportReason;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reviews")
public class ReviewReportController {

    private final ReviewReportService reportService;

    @PostMapping("/{reviewId}/report")
    @ResponseBody
    public ResponseEntity<?> reportReview(
            @PathVariable UUID reviewId,
            @RequestParam("reason") ReviewReportReason reason,
            @RequestParam(value = "note", required = false) String note,
            @CurrentUser User user,
            HttpServletRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(401).body("Vui lòng đăng nhập để báo cáo đánh giá.");
        }
        try {
            reportService.reportReview(
                    reviewId,
                    user,
                    request.getRemoteAddr(),
                    new ReportReviewRequest(reason, note)
            );

            // Trả về 200 OK nếu thành công
            return ResponseEntity.ok("Gửi báo cáo thành công!");

        } catch (IllegalStateException | IllegalArgumentException ex) {
            // Trả về 400 Bad Request nếu trùng lặp hoặc thiếu dữ liệu
            // Frontend sẽ nhảy vào block 'else' và hiện alert lỗi
            return ResponseEntity.badRequest().body(ex.getMessage());

        } catch (Exception ex) {
            // Trả về 500 cho các lỗi khác
            return ResponseEntity.internalServerError().body("Đã xảy ra lỗi hệ thống.");
        }
    }

    @PutMapping("/{reviewId}/report")
    @ResponseBody
    public ResponseEntity<?> updateReport(
            @PathVariable UUID reviewId,
            @ModelAttribute UpdateReportReviewRequest request,
            @CurrentUser User user
    ) {
        if (user == null) {
            return ResponseEntity.status(401).body("Vui lòng đăng nhập để cập nhật báo cáo.");
        }
        try {
            reportService.updateReport(reviewId, user, request);
            return ResponseEntity.ok("Cập nhật báo cáo thành công!");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            // Trả về 400 Bad Request kèm lý do (VD: Đã sửa 1 lần rồi)
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống.");
        }
    }

}