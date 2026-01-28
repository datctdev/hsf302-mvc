package com.hsf.e_comerce.review.controller;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.review.dto.request.ReportReviewRequest;
import com.hsf.e_comerce.review.service.ReviewReportService;
import com.hsf.e_comerce.review.valueobject.ReviewReportReason;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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
    public String reportReview(
            @PathVariable UUID reviewId,
            @RequestParam("reason") ReviewReportReason reason,
            @RequestParam(value = "note", required = false) String note,
            @CurrentUser User user,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        try {
            reportService.reportReview(
                    reviewId,
                    user,
                    request.getRemoteAddr(),
                    new ReportReviewRequest(reason, note)
            );
            redirectAttributes.addFlashAttribute("success", "Đã gửi báo cáo đánh giá");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:" + request.getHeader("Referer");
    }


}

