package com.hsf.e_comerce.review.controller;

import com.hsf.e_comerce.review.service.ReviewReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewReportController {

    private final ReviewReportService reportService;

    @GetMapping("/reports")
    public String listReportedReviews(Model model) {

        model.addAttribute(
                "reportedReviews",
                reportService.getPendingReportedReviews()
        );

        return "admin/review-reports";
    }

    @PostMapping("/{reviewId}/hide")
    public String hideReview(@PathVariable UUID reviewId) {
        reportService.hideReview(reviewId);
        return "redirect:/admin/reviews/reports";
    }

    @PostMapping("/{reviewId}/ignore")
    public String ignoreReview(@PathVariable UUID reviewId) {
        reportService.ignoreReview(reviewId);
        return "redirect:/admin/reviews/reports";
    }

}
