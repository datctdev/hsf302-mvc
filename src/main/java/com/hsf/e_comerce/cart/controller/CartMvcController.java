package com.hsf.e_comerce.cart.controller;

import com.hsf.e_comerce.cart.dto.request.AddToCartRequest;
import com.hsf.e_comerce.cart.dto.request.UpdateCartItemRequest;
import com.hsf.e_comerce.cart.dto.response.CartResponse;
import com.hsf.e_comerce.cart.service.CartService;
import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.auth.entity.User;
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
@RequestMapping("/cart")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CartMvcController {

    private final CartService cartService;

    @GetMapping
    public String viewCart(@CurrentUser User currentUser, Model model) {
        CartResponse cart = cartService.getCartByUser(currentUser);
        model.addAttribute("cart", cart);
        return "cart/cart";
    }

    @PostMapping("/add")
    public String addToCart(
            @CurrentUser User currentUser,
            @Valid @ModelAttribute("addToCartRequest") AddToCartRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng kiểm tra lại thông tin.");
            return "redirect:/products/" + request.getProductId();
        }

        try {
            cartService.addToCart(currentUser, request);
            redirectAttributes.addFlashAttribute("success", "Đã thêm sản phẩm vào giỏ hàng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/products/" + request.getProductId();
    }

    @PostMapping("/update")
    public String updateCartItem(
            @CurrentUser User currentUser,
            @Valid @ModelAttribute("updateCartItemRequest") UpdateCartItemRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng kiểm tra lại thông tin.");
            return "redirect:/cart";
        }

        try {
            cartService.updateCartItem(currentUser, request);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật giỏ hàng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/cart";
    }

    @PostMapping("/remove/{cartItemId}")
    public String removeCartItem(
            @CurrentUser User currentUser,
            @PathVariable UUID cartItemId,
            RedirectAttributes redirectAttributes) {
        
        try {
            cartService.removeCartItem(currentUser, cartItemId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa sản phẩm khỏi giỏ hàng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clearCart(
            @CurrentUser User currentUser,
            RedirectAttributes redirectAttributes) {
        
        try {
            cartService.clearCart(currentUser);
            redirectAttributes.addFlashAttribute("success", "Đã xóa tất cả sản phẩm khỏi giỏ hàng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/cart";
    }
}
