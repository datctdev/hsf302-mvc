package com.hsf.e_comerce.seller.controller;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.seller.dto.request.RejectSellerRequestRequest;
import com.hsf.e_comerce.seller.dto.response.SellerRequestResponse;
import com.hsf.e_comerce.seller.service.SellerRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/seller-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSellerMvcController {

    private final SellerRequestService sellerRequestService;

    @GetMapping
    public String getAllRequests(
            @RequestParam(required = false) String status,
            Model model) {
        
        List<SellerRequestResponse> requests;
        if (status != null && !status.isEmpty()) {
            requests = sellerRequestService.getRequestsByStatus(status);
        } else {
            requests = sellerRequestService.getAllRequests();
        }
        
        model.addAttribute("requests", requests);
        model.addAttribute("status", status);
        return "admin/seller-requests";
    }

    @GetMapping("/{id}")
    public String getRequestById(@PathVariable UUID id, Model model) {
        try {
            SellerRequestResponse request = sellerRequestService.getRequestById(id);
            model.addAttribute("request", request);
            return "admin/seller-request-detail";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/admin/seller-requests";
        }
    }

    @PostMapping("/{id}/approve")
    public String approveRequest(
            @PathVariable UUID id,
            @CurrentUser User admin,
            RedirectAttributes redirectAttributes) {
        
        try {
            sellerRequestService.approveRequest(id, admin.getId());
            redirectAttributes.addFlashAttribute("success", "Đã duyệt yêu cầu thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/admin/seller-requests";
    }

    @PostMapping("/{id}/reject")
    public String rejectRequest(
            @PathVariable UUID id,
            @CurrentUser User admin,
            @RequestParam("rejectionReason") String rejectionReason,
            RedirectAttributes redirectAttributes) {
        
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng nhập lý do từ chối");
            return "redirect:/admin/seller-requests";
        }

        try {
            RejectSellerRequestRequest request = new RejectSellerRequestRequest();
            request.setRejectionReason(rejectionReason.trim());
            sellerRequestService.rejectRequest(id, admin.getId(), request);
            redirectAttributes.addFlashAttribute("success", "Đã từ chối yêu cầu");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/admin/seller-requests";
    }
}
