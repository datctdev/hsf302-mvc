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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String updateReport(
            @PathVariable UUID reviewId,
            @ModelAttribute UpdateReportReviewRequest request,
            @CurrentUser User user,
            HttpServletRequest httpRequest,
            RedirectAttributes redirect
    ) {
        try {
            reportService.updateReport(reviewId, user, request);
            redirect.addFlashAttribute("success", "Đã cập nhật báo cáo");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:" + httpRequest.getHeader("Referer");
    }

}