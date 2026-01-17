package com.hsf.e_comerce.order.controller;

import com.hsf.e_comerce.cart.entity.Cart;
import com.hsf.e_comerce.cart.entity.CartItem;
import com.hsf.e_comerce.cart.repository.CartRepository;
import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.order.dto.request.CreateOrderRequest;
import com.hsf.e_comerce.order.dto.response.OrderResponse;
import com.hsf.e_comerce.order.service.OrderService;
import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.shop.entity.Shop;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OrderMvcController {

    private final OrderService orderService;
    private final CartRepository cartRepository;

    @GetMapping("/checkout")
    public String checkout(@CurrentUser User currentUser, Model model) {
        // Load cart with items, products and shops to group by shop
        Cart cart = cartRepository.findByUserIdWithItemsAndProducts(currentUser.getId())
                .orElse(null);
        
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            model.addAttribute("error", "Giỏ hàng trống.");
            return "redirect:/cart";
        }

        // Group cart items by shop
        Map<Shop, List<CartItem>> itemsByShop = cart.getItems().stream()
                .collect(Collectors.groupingBy(item -> item.getProduct().getShop()));

        // Calculate subtotal for each shop
        Map<UUID, BigDecimal> shopSubtotals = new HashMap<>();
        for (Map.Entry<Shop, List<CartItem>> entry : itemsByShop.entrySet()) {
            BigDecimal subtotal = entry.getValue().stream()
                    .map(item -> item.getTotalPrice())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            shopSubtotals.put(entry.getKey().getId(), subtotal);
        }

        // Get first shop for checkout (one order per shop for now)
        Shop firstShop = itemsByShop.isEmpty() ? null : itemsByShop.keySet().iterator().next();
        BigDecimal firstShopSubtotal = firstShop != null ? shopSubtotals.get(firstShop.getId()) : BigDecimal.ZERO;

        model.addAttribute("itemsByShop", itemsByShop);
        model.addAttribute("firstShop", firstShop);
        model.addAttribute("firstShopSubtotal", firstShopSubtotal);
        model.addAttribute("createOrderRequest", new CreateOrderRequest());
        return "orders/checkout";
    }

    @PostMapping("/create")
    public String createOrder(
            @CurrentUser User currentUser,
            @Valid @ModelAttribute("createOrderRequest") CreateOrderRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng kiểm tra lại thông tin.");
            return "redirect:/orders/checkout";
        }

        try {
            OrderResponse order = orderService.createOrder(currentUser, request);
            redirectAttributes.addFlashAttribute("success", "Đặt hàng thành công! Mã đơn hàng: " + order.getOrderNumber());
            return "redirect:/orders/" + order.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/orders/checkout";
        }
    }

    @GetMapping
    public String orderHistory(@CurrentUser User currentUser, Model model) {
        List<OrderResponse> orders = orderService.getOrdersByUser(currentUser);
        model.addAttribute("orders", orders);
        return "orders/history";
    }

    @GetMapping("/{id}")
    public String orderDetail(
            @CurrentUser User currentUser,
            @PathVariable java.util.UUID id,
            Model model) {
        try {
            OrderResponse order = orderService.getOrderByIdAndUser(id, currentUser);
            model.addAttribute("order", order);
            return "orders/detail";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/orders";
        }
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(
            @CurrentUser User currentUser,
            @PathVariable java.util.UUID id,
            RedirectAttributes redirectAttributes) {
        try {
            orderService.cancelOrder(id, currentUser);
            redirectAttributes.addFlashAttribute("success", "Đã hủy đơn hàng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/" + id;
    }
}
