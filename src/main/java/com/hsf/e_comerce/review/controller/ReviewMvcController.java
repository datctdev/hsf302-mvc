package com.hsf.e_comerce.review.controller;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.product.dto.response.ProductResponse;
import com.hsf.e_comerce.product.service.ProductService;
import com.hsf.e_comerce.review.dto.request.CreateReviewRequest;
import com.hsf.e_comerce.review.dto.request.UpdateReviewRequest;
import com.hsf.e_comerce.review.dto.response.ReviewResponse;
import com.hsf.e_comerce.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ReviewMvcController {

    private final ReviewService reviewService;
    private final ProductService productService;

    // 1. Hiển thị form viết đánh giá (Nhận subOrderId từ URL)
    @GetMapping("/products/{productId}/review")
    @PreAuthorize("hasRole('BUYER')")
    public String showCreateForm(
            @PathVariable UUID productId,
            @RequestParam(required = false) UUID subOrderId, // Nhận ID đơn hàng từ nút bấm
            Model model) {

        // Lấy thông tin sản phẩm để hiển thị tên, ảnh
        ProductResponse product = productService.getPublishedProductById(productId);
        model.addAttribute("product", product);

        // Tạo object form và gán sẵn subOrderId
        CreateReviewRequest request = new CreateReviewRequest();
        request.setSubOrderId(subOrderId);

        model.addAttribute("createReviewRequest", request);
        return "reviews/create";
    }

    // 2. Xử lý khi người dùng bấm nút "Gửi đánh giá"
    @PostMapping("/products/{productId}/review")
    @PreAuthorize("hasRole('BUYER')")
    public String createReview(
            @PathVariable UUID productId,
            @CurrentUser User user,
            @Valid @ModelAttribute("createReviewRequest") CreateReviewRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng kiểm tra lại thông tin (số sao, nội dung)");
            // Nếu lỗi, quay lại form kèm theo subOrderId cũ
            return "redirect:/products/" + productId + "/review?subOrderId=" + request.getSubOrderId();
        }

        try {
            reviewService.createReview(user, productId, request);
            redirectAttributes.addFlashAttribute("success", "Cảm ơn bạn! Đánh giá đã được đăng thành công.");
            // Đánh giá xong thì quay về trang chi tiết sản phẩm để xem thành quả
            return "redirect:/products/" + productId;
        } catch (Exception e) {
            // Nếu lỗi (vd: đã đánh giá rồi), thông báo lỗi
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/products/" + productId + "/review?subOrderId=" + request.getSubOrderId();
        }
    }

    // 3. Hiển thị form sửa đánh giá
    @GetMapping("/reviews/{reviewId}/edit")
    @PreAuthorize("hasRole('BUYER')")
    public String showEditForm(@PathVariable UUID reviewId, @CurrentUser User user, Model model) {
        try {
            ReviewResponse review = reviewService.getReviewById(reviewId);

            // Map dữ liệu cũ vào form edit
            UpdateReviewRequest updateRequest = new UpdateReviewRequest();
            updateRequest.setRating(review.getRating());
            updateRequest.setComment(review.getComment());

            model.addAttribute("review", review);
            model.addAttribute("updateReviewRequest", updateRequest);
            return "reviews/edit";
        } catch (Exception e) {
            return "redirect:/orders"; // Lỗi thì quay về danh sách đơn
        }
    }

    // Endpoint tìm review của tôi để xem/sửa
    @GetMapping("/reviews/my-review")
    @PreAuthorize("hasRole('BUYER')")
    public String findMyReview(
            @RequestParam UUID productId,
            @RequestParam UUID subOrderId,
            @CurrentUser User user) {

        UUID reviewId = reviewService.getReviewIdByUserAndProductAndSubOrder(user.getId(), productId, subOrderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));
        return "redirect:/reviews/" + reviewId + "/edit";
    }

    // 4. Xử lý cập nhật đánh giá
    @PostMapping("/reviews/{reviewId}/edit")
    @PreAuthorize("hasRole('BUYER')")
    public String updateReview(
            @PathVariable UUID reviewId,
            @CurrentUser User user,
            @ModelAttribute UpdateReviewRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            ReviewResponse review = reviewService.updateReview(user, reviewId, request);
            redirectAttributes.addFlashAttribute("success", "Cập nhật đánh giá thành công");
            return "redirect:/products/" + review.getProductId(); // Tạm thời về trang chủ
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/reviews/" + reviewId + "/edit";
        }
    }

    // 5. Xóa đánh giá
    @PostMapping("/reviews/{reviewId}/delete")
    @PreAuthorize("hasRole('BUYER')")
    public String deleteReview(@PathVariable UUID reviewId, @CurrentUser User user, RedirectAttributes redirectAttributes) {
        try {
            reviewService.deleteReview(user, reviewId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa đánh giá");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders"; // Xóa xong quay về lịch sử đơn hàng
    }
}