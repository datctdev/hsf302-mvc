package com.hsf.e_comerce.review.controller;

import com.hsf.e_comerce.review.dto.response.ReportedReviewResponse;
import com.hsf.e_comerce.review.service.ReviewReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
}
