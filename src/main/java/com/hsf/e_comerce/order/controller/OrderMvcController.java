package com.hsf.e_comerce.order.controller;

import com.hsf.e_comerce.cart.dto.response.CartItemResponse;
import com.hsf.e_comerce.cart.entity.Cart;
import com.hsf.e_comerce.cart.entity.CartItem;
import com.hsf.e_comerce.cart.repository.CartRepository;
import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.order.dto.request.CreateOrderRequest;
import com.hsf.e_comerce.order.dto.response.OrderResponse;
import com.hsf.e_comerce.order.service.OrderItemService;
import com.hsf.e_comerce.order.service.OrderService;
import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.shop.dto.response.ShopResponse;
import com.hsf.e_comerce.shop.entity.Shop;
import com.hsf.e_comerce.shop.service.ShopService;
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
    private final OrderItemService orderItemService;
    private final ShopService shopService;

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
            return "redirect:/payments/" + order.getId();
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

    @GetMapping("/{orderId}/edit-checkout")
    public String editCheckout(
            @PathVariable UUID orderId,
            @CurrentUser User currentUser,
            Model model
    ) {
        try {
            // 1. Lấy thông tin order
            OrderResponse order = orderService.getOrderForEditCheckout(orderId, currentUser);
            UUID shopId = order.getShopId();

            // 2. Lấy items theo order
            Map<UUID, List<CartItemResponse>> itemsByShop = orderItemService.getItemsByOrder(orderId);

            // ✅ CHECK NGAY SAU KHI LẤY DATA
            if (itemsByShop == null || itemsByShop.isEmpty()) {
                model.addAttribute("error", "Không có sản phẩm trong đơn hàng.");
                return "redirect:/orders";
            }

            // 3. Lấy items của shop cụ thể
            List<CartItemResponse> shopItems = itemsByShop.get(shopId);

            // ✅ CHECK SHOPITEMS
            if (shopItems == null || shopItems.isEmpty()) {
                model.addAttribute("error", "Shop không có sản phẩm trong đơn");
                return "redirect:/orders";
            }

            // 4. Lấy thông tin shop
            Shop firstShop = shopService.getShop(shopId)
                    .orElseThrow(() -> new RuntimeException("Shop không tồn tại"));

            // 5. Tính subtotal
            BigDecimal firstShopSubtotal = shopItems.stream()
                    .map(CartItemResponse::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 6. Add attributes
            model.addAttribute("order", order);
            model.addAttribute("itemsByShop", itemsByShop);
            model.addAttribute("shopItems", shopItems);
            model.addAttribute("firstShop", firstShop);
            model.addAttribute("firstShopSubtotal", firstShopSubtotal);

            // ✅ DEBUG
            // Thêm vào editCheckout() để debug
            System.out.println("=== DEBUG EDIT CHECKOUT ===");
            System.out.println("Order ID: " + orderId);
            System.out.println("Shop ID: " + shopId);
            System.out.println("Items by shop keys: " + itemsByShop.keySet());
            System.out.println("Shop items count: " + (shopItems != null ? shopItems.size() : 0));
            System.out.println("Order shipping address: " + order.getShippingAddress());
            System.out.println("Order shipping fee: " + order.getShippingFee());

            CreateOrderRequest createOrderRequest = CreateOrderRequest.fromOrder(order);
            System.out.println("=== DEBUG CREATE ORDER REQUEST ===");
            System.out.println("Shipping Fee: " + createOrderRequest.getShippingFee());
            System.out.println("Shipping Name: " + createOrderRequest.getShippingName());
            System.out.println("Shipping Address: " + createOrderRequest.getShippingAddress());
            System.out.println("Shipping District ID: " + createOrderRequest.getShippingDistrictId());
            System.out.println("Shipping Ward Code: " + createOrderRequest.getShippingWardCode());
            model.addAttribute("createOrderRequest", createOrderRequest);


            return "orders/checkout-edit";

        } catch (Exception e) {
            model.addAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/orders";
        }
    }



    @PostMapping("/{orderId}/update-checkout")
    public String updateCheckout(
            @PathVariable UUID orderId,
            @Valid @ModelAttribute("createOrderRequest") CreateOrderRequest request,
            BindingResult bindingResult,
            @CurrentUser User user,
            Model model   // ✅ BẮT BUỘC
    ) {

        if (bindingResult.hasErrors()) {

            OrderResponse order = orderService.getOrderForEditCheckout(orderId, user);
            UUID shopId = order.getShopId();

            Map<UUID, List<CartItemResponse>> itemsByShop =
                    orderItemService.getItemsByOrder(orderId);

            List<CartItemResponse> shopItems = itemsByShop.get(shopId);
            Shop firstShop = shopService.getShop(shopId).orElse(null);

            model.addAttribute("order", order);
            model.addAttribute("createOrderRequest", request); // giữ data user nhập
            model.addAttribute("shopItems", shopItems);
            model.addAttribute("itemsByShop", itemsByShop);
            model.addAttribute("firstShop", firstShop);
            model.addAttribute("firstShopSubtotal",
                    shopItems.stream()
                            .map(CartItemResponse::getTotalPrice)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
            );
            model.addAttribute("error", "Thông tin không hợp lệ");

            return "orders/checkout-edit"; // ✅ render lại view
        }

        orderService.updateCheckoutInfo(orderId, request, user);
        return "redirect:/payments/" + orderId;
    }
}
