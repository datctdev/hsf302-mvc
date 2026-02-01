package com.hsf.e_comerce.review.controller;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.review.service.SellerReviewReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class SellerReviewController {

    private final SellerReviewReplyService replyService;

    @PostMapping("/seller/reviews/{reviewId}/reply")
    public String replyToReview(
            @PathVariable UUID reviewId,
            @CurrentUser User seller,
            @RequestParam String reply,
            RedirectAttributes redirectAttributes
    ) {
        try {
            UUID productId = replyService.replyToReview(seller, reviewId, reply);
            redirectAttributes.addFlashAttribute("success", "Đã phản hồi đánh giá");
            return "redirect:/products/" + productId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }

    @PostMapping("/seller/reviews/{reviewId}/reply/edit")
    public String updateReply(
            @PathVariable UUID reviewId,
            @CurrentUser User seller,
            @RequestParam String reply,
            RedirectAttributes redirectAttributes
    ) {
        try {
            UUID productId = replyService.updateReply(seller, reviewId, reply);
            redirectAttributes.addFlashAttribute("success",
                    "Cập nhật phản hồi thành công");
            return "redirect:/products/" + productId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }
}
