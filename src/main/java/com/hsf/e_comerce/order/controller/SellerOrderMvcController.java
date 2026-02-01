package com.hsf.e_comerce.order.controller;

import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.order.dto.request.UpdateOrderStatusRequest;
import com.hsf.e_comerce.order.dto.response.OrderResponse;
import com.hsf.e_comerce.order.service.OrderService;
import com.hsf.e_comerce.order.valueobject.OrderStatus;
import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.common.exception.CustomException;
import com.hsf.e_comerce.shop.dto.response.ShopResponse;
import com.hsf.e_comerce.shop.service.ShopService;
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
@RequestMapping("/seller/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class SellerOrderMvcController {

    private final OrderService orderService;
    private final ShopService shopService;

    @GetMapping
    public String sellerOrders(
            @CurrentUser User currentUser,
            @RequestParam(required = false) String status,
            Model model) {

        ShopResponse shop;
        try {
            shop = shopService.getShopByUserId(currentUser.getId());
        } catch (CustomException e) {
            return "redirect:/seller/become-seller";
        }
        UUID shopId = shop.getId();

        List<OrderResponse> orders;
        if (status != null && !status.isEmpty()) {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                orders = orderService.getOrdersByShopAndStatus(shopId, orderStatus);
            } catch (IllegalArgumentException e) {
                orders = orderService.getOrdersByShop(shopId);
            }
        } else {
            orders = orderService.getOrdersByShop(shopId);
        }

        model.addAttribute("orders", orders);
        model.addAttribute("currentStatus", status);
        model.addAttribute("orderStatuses",
                List.of(OrderStatus.values()).stream()
                        .filter(s -> s != OrderStatus.PENDING_PAYMENT)
                        .toList()
        );

        return "seller/orders";
    }

    @GetMapping("/{id}")
    public String sellerOrderDetail(
            @CurrentUser User currentUser,
            @PathVariable UUID id,
            Model model) {

        ShopResponse shop;
        try {
            shop = shopService.getShopByUserId(currentUser.getId());
        } catch (CustomException e) {
            return "redirect:/seller/become-seller";
        }
        UUID shopId = shop.getId();

        try {
            OrderResponse order = orderService.getOrderByIdAndShop(id, shopId);
            model.addAttribute("order", order);
            model.addAttribute("orderStatuses", orderService.getAllowedNextStatuses(order.getStatus()));
            model.addAttribute("updateOrderStatusRequest", new UpdateOrderStatusRequest());
            return "seller/order-detail";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/seller/orders";
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
            return "redirect:/seller/orders/" + id;
        }

        try {
            orderService.updateOrderStatus(id, request, currentUser);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật trạng thái đơn hàng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/seller/orders/" + id;
    }

    @PostMapping("/{id}/create-ghn")
    public String retryCreateGhnOrder(
            @CurrentUser User currentUser,
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes) {
        try {
            orderService.retryCreateGhnOrder(id, currentUser);
            redirectAttributes.addFlashAttribute("success", "Đã tạo vận đơn GHN thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/orders/" + id;
    }

    @PostMapping("/{id}/set-ghn-code")
    public String setGhnOrderCode(
            @CurrentUser User currentUser,
            @PathVariable UUID id,
            @RequestParam String ghnOrderCode,
            RedirectAttributes redirectAttributes) {
        try {
            orderService.setGhnOrderCodeManually(id, ghnOrderCode, currentUser);
            redirectAttributes.addFlashAttribute("success", "Đã lưu mã vận đơn GHN.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/orders/" + id;
    }
}
