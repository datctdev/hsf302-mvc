package com.hsf.e_comerce.shop.controller;

import com.hsf.e_comerce.product.dto.response.ProductResponse;
import com.hsf.e_comerce.product.service.ProductService;
import com.hsf.e_comerce.shop.entity.Shop;
import com.hsf.e_comerce.shop.service.ShopService;
import com.hsf.e_comerce.shop.valueobject.ShopStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Trang shop công khai cho buyer: xem thông tin shop và danh sách sản phẩm đã xuất bản.
 * Route /shops/{id} – không cần đăng nhập.
 */
@Controller
@RequestMapping("/shops")
@RequiredArgsConstructor
public class PublicShopMvcController {

    private final ShopService shopService;
    private final ProductService productService;

    @GetMapping("/{id}")
    public String viewShop(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            Model model) {

        Shop shop = shopService.getShop(id).orElse(null);
        if (shop == null || shop.getStatus() != ShopStatus.ACTIVE) {
            return "redirect:/products";
        }

        int safeSize = Math.min(Math.max(1, size), 48); // default 24 from @RequestParam
        Page<ProductResponse> productPage = productService.getPublishedProductsByShop(id, page, safeSize);

        model.addAttribute("shop", shop);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("productPage", productPage);
        return "shops/view";
    }
}
