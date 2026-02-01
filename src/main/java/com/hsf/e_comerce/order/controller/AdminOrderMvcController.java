package com.hsf.e_comerce.order.controller;

import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.order.dto.request.UpdateOrderStatusRequest;
import com.hsf.e_comerce.order.dto.response.OrderResponse;
import com.hsf.e_comerce.order.service.OrderService;
import com.hsf.e_comerce.order.valueobject.OrderStatus;
import com.hsf.e_comerce.auth.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderMvcController {

    private final OrderService orderService;

    @GetMapping
    public String adminOrders(
            @RequestParam(required = false) String status,
            Model model) {
        
        List<OrderResponse> orders;
        if (status != null && !status.isEmpty()) {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                orders = orderService.getOrdersByStatus(orderStatus);
            } catch (IllegalArgumentException e) {
                orders = orderService.getAllOrders();
            }
        } else {
            orders = orderService.getAllOrders();
        }

        model.addAttribute("orders", orders);
        model.addAttribute("currentStatus", status);
        model.addAttribute("orderStatuses", OrderStatus.values());
        return "admin/orders";
    }

    @GetMapping("/{id}")
    public String adminOrderDetail(
            @PathVariable UUID id,
            Model model) {
        try {
            OrderResponse order = orderService.getOrderById(id);
            model.addAttribute("order", order);
            model.addAttribute("orderStatuses", orderService.getAllowedNextStatuses(order.getStatus()));
            model.addAttribute("updateOrderStatusRequest", new UpdateOrderStatusRequest());
            return "admin/order-detail";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/admin/orders";
        }
    }

    @PostMapping("/{id}/update-status")
    public String updateOrderStatus(
            @CurrentUser User currentUser,
            @PathVariable UUID id,
            @Valid @ModelAttribute("updateOrderStatusRequest") UpdateOrderStatusRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng kiểm tra lại thông tin.");
            return "redirect:/admin/orders/" + id;
        }

        try {
            orderService.updateOrderStatus(id, request, currentUser);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật trạng thái đơn hàng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/orders/" + id;
    }
}
