package com.hsf.e_comerce.product.controller;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.common.exception.CustomException;
import com.hsf.e_comerce.product.dto.request.CreateProductRequest;
import com.hsf.e_comerce.product.dto.request.UpdateProductRequest;
import com.hsf.e_comerce.product.dto.response.ProductResponse;
import com.hsf.e_comerce.product.service.ProductService;
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

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/seller/products")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
public class SellerProductMvcController {

    private final ProductService productService;
    private final ShopService shopService;

    @GetMapping
    public String getAllProducts(
            @CurrentUser User user,
            @RequestParam(required = false) String status,
            Model model) {
        
        UUID shopId = getShopIdByUser(user);
        
        List<ProductResponse> products;
        if (status != null && !status.isEmpty()) {
            products = productService.getProductsByShopAndStatus(shopId, status);
        } else {
            products = productService.getAllProductsByShop(shopId);
        }
        
        model.addAttribute("products", products);
        model.addAttribute("status", status);
        return "seller/products";
    }

    @GetMapping("/add")
    public String showAddProductForm(
            @CurrentUser User user,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        // Kiểm tra shop có địa chỉ đầy đủ chưa
        UUID shopId = getShopIdByUser(user);
        Shop shop = shopService.getShop(shopId).orElseThrow(() -> new CustomException("Shop không tồn tại"));
        if (shop.getDistrictId() == null || shop.getWardCode() == null || shop.getWardCode().isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                "Vui lòng cập nhật địa chỉ shop đầy đủ (Tỉnh/Thành, Quận/Huyện, Phường/Xã) trước khi thêm sản phẩm. " +
                "Địa chỉ này cần thiết để tính phí vận chuyển GHN.");
            return "redirect:/seller/shop";
        }

        if (!model.containsAttribute("createProductRequest")) {
            model.addAttribute("createProductRequest", new CreateProductRequest());
        }
        model.addAttribute("categories", productService.findAllCategory());
        
        return "seller/products-add";
    }

    @PostMapping("/add")
    public String createProduct(
            @CurrentUser User user,
            @Valid @ModelAttribute("createProductRequest") CreateProductRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.createProductRequest", bindingResult);
            redirectAttributes.addFlashAttribute("createProductRequest", request);
            redirectAttributes.addFlashAttribute("error", "Vui lòng kiểm tra lại thông tin sản phẩm");
            return "redirect:/seller/products/add";
        }

        try {
            UUID shopId = getShopIdByUser(user);
            Shop shop = shopService.getShop(shopId).orElseThrow(() -> new CustomException("Shop không tồn tại"));
            if (shop.getDistrictId() == null || shop.getWardCode() == null || shop.getWardCode().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", 
                    "Vui lòng cập nhật địa chỉ shop đầy đủ (Tỉnh/Thành, Quận/Huyện, Phường/Xã) trước khi thêm sản phẩm. " +
                    "Địa chỉ này cần thiết để tính phí vận chuyển GHN.");
                return "redirect:/seller/shop";
            }
            
            productService.createProduct(shopId, request);
            redirectAttributes.addFlashAttribute("success", "Thêm sản phẩm thành công");
            return "redirect:/seller/products";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("createProductRequest", request);
            return "redirect:/seller/products/add";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditProductForm(
            @CurrentUser User user,
            @PathVariable UUID id,
            Model model) {
        
        try {
            UUID shopId = getShopIdByUser(user);
            ProductResponse product = productService.getProductById(id);
            
            // Verify product belongs to seller's shop
            if (!product.getShopId().equals(shopId)) {
                model.addAttribute("error", "Bạn không có quyền chỉnh sửa sản phẩm này");
                return "redirect:/seller/products";
            }
            
            if (!model.containsAttribute("updateProductRequest")) {
                UpdateProductRequest updateRequest = new UpdateProductRequest();
                updateRequest.setName(product.getName());
                updateRequest.setDescription(product.getDescription());
                updateRequest.setBasePrice(product.getBasePrice());
                updateRequest.setStatus(product.getStatus());
                updateRequest.setCategoryId(product.getCategoryId());
                model.addAttribute("updateProductRequest", updateRequest);
            }
            model.addAttribute("categories", productService.findAllCategory());
            model.addAttribute("product", product);
            return "seller/products-edit";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/seller/products";
        }
    }

    @PostMapping("/edit/{id}")
    public String updateProduct(
            @CurrentUser User user,
            @PathVariable UUID id,
            @Valid @ModelAttribute("updateProductRequest") UpdateProductRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.updateProductRequest", bindingResult);
            redirectAttributes.addFlashAttribute("updateProductRequest", request);
            redirectAttributes.addFlashAttribute("error", "Vui lòng kiểm tra lại thông tin sản phẩm");
            return "redirect:/seller/products/edit/" + id;
        }

        try {
            UUID shopId = getShopIdByUser(user);
            productService.updateProduct(shopId, id, request);
            redirectAttributes.addFlashAttribute("success", "Cập nhật sản phẩm thành công");
            return "redirect:/seller/products";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("updateProductRequest", request);
            return "redirect:/seller/products/edit/" + id;
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteProduct(
            @CurrentUser User user,
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes) {
        
        try {
            UUID shopId = getShopIdByUser(user);
            productService.deleteProduct(shopId, id);
            redirectAttributes.addFlashAttribute("success", "Xóa sản phẩm thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/seller/products";
    }

    private UUID getShopIdByUser(User user) {
        return shopService.getShopByUserId(user.getId()).getId();
    }
}
