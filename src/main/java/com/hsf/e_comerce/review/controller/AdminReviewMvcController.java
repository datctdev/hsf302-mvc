package com.hsf.e_comerce.review.controller;

import com.hsf.e_comerce.review.entity.Review;
import com.hsf.e_comerce.review.service.ReviewReportService;
import com.hsf.e_comerce.review.service.ReviewService;
import com.hsf.e_comerce.review.valueobject.ReviewStatus;
import com.hsf.e_comerce.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewMvcController {

    private final ReviewService reviewService;
    private final ShopService shopService;
    private final ReviewReportService reportService;

    @GetMapping
    public String index(
            @RequestParam(required = false, defaultValue = "list") String tab,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) UUID shopId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            Model model) {

        // TAB 1: Danh sách Review
        if ("list".equals(tab)) {
            Page<Review> reviewPage = reviewService.getAllReviewsForAdmin(keyword, rating, status, shopId, page, size);
            model.addAttribute("reviewPage", reviewPage);

            // Các thuộc tính cho bộ lọc
            model.addAttribute("keyword", keyword);
            model.addAttribute("currentRating", rating);
            model.addAttribute("currentStatus", status);
            model.addAttribute("currentShopId", shopId);
            model.addAttribute("statuses", ReviewStatus.values());
            model.addAttribute("shops", shopService.findAllShops());
        }
        // TAB 2: Danh sách Báo cáo
        else if ("reports".equals(tab)) {
            model.addAttribute("reportedReviews", reportService.getPendingReportedReviews());
        }

        // Dữ liệu chung cho cả 2 tab
        model.addAttribute("pendingReportCount", reportService.countPendingReportedReviews());
        model.addAttribute("currentTab", tab);

        return "admin/reviews";
    }

    // --- ACTIONS: ẨN/HIỆN REVIEW (Dùng cho Tab List) ---
    @PostMapping("/{reviewId}/toggle-status")
    public String toggleStatus(@PathVariable UUID reviewId, RedirectAttributes redirectAttributes) {
        try {
            reviewService.toggleReviewVisibility(reviewId);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật trạng thái đánh giá.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/reviews?tab=list";
    }

    // --- ACTIONS: XỬ LÝ BÁO CÁO (Dùng cho Tab Reports) ---

    // 1. Phạt: Ẩn review theo báo cáo
    @PostMapping("/{reviewId}/hide-report")
    public String hideReviewFromReport(@PathVariable UUID reviewId, RedirectAttributes redirectAttributes) {
        try {
            reportService.hideReview(reviewId);
            redirectAttributes.addFlashAttribute("success", "Đã ẩn đánh giá vi phạm.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/reviews?tab=reports";
    }

    // 2. Bỏ qua: Review hợp lệ
    @PostMapping("/{reviewId}/ignore-report")
    public String ignoreReport(@PathVariable UUID reviewId, RedirectAttributes redirectAttributes) {
        try {
            reportService.ignoreReview(reviewId);
            redirectAttributes.addFlashAttribute("success", "Đã bỏ qua báo cáo.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/reviews?tab=reports";
    }
}