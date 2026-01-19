package com.hsf.e_comerce.seller.controller;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.file.service.FileService;
import com.hsf.e_comerce.seller.dto.request.SellerRequestRequest;
import com.hsf.e_comerce.seller.dto.response.SellerRequestResponse;
import com.hsf.e_comerce.seller.service.SellerRequestService;
import com.hsf.e_comerce.shop.dto.request.UpdateShopRequest;
import com.hsf.e_comerce.shop.dto.response.ShopResponse;
import com.hsf.e_comerce.shop.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/seller")
@RequiredArgsConstructor
public class SellerMvcController {

    private final SellerRequestService sellerRequestService;
    private final ShopService shopService;
    private final FileService fileService;

    @GetMapping("/become-seller")
    public String showBecomeSellerForm(@CurrentUser User user, Model model) {
        if (user == null) {
            return "redirect:/login";
        }

        // Check if already seller
        if (sellerRequestService.isSeller(user.getId())) {
            model.addAttribute("message", "Bạn đã là seller");
            return "redirect:/seller/shop";
        }

        // Check if has pending request
        if (sellerRequestService.hasPendingRequest(user.getId())) {
            SellerRequestResponse request = sellerRequestService.getRequestByUserId(user.getId());
            model.addAttribute("request", request);
            model.addAttribute("message", "Bạn có request đang chờ duyệt");
        }

        if (!model.containsAttribute("sellerRequestRequest")) {
            model.addAttribute("sellerRequestRequest", new SellerRequestRequest());
        }

        return "seller/become-seller";
    }

    @PostMapping("/become-seller")
    public String createSellerRequest(
            @CurrentUser User user,
            @Valid @ModelAttribute("sellerRequestRequest") SellerRequestRequest request,
            @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
            @RequestParam(value = "coverImageFile", required = false) MultipartFile coverImageFile,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        
        if (user == null) {
            return "redirect:/login";
        }

        // Handle logo file upload
        if (logoFile != null && !logoFile.isEmpty()) {
            try {
                String contentType = logoFile.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    redirectAttributes.addFlashAttribute("error", "Logo phải là file ảnh");
                    redirectAttributes.addFlashAttribute("sellerRequestRequest", request);
                    return "redirect:/seller/become-seller";
                }

                if (logoFile.getSize() > 25 * 1024 * 1024) {
                    redirectAttributes.addFlashAttribute("error", "Kích thước logo không được vượt quá 25MB");
                    redirectAttributes.addFlashAttribute("sellerRequestRequest", request);
                    return "redirect:/seller/become-seller";
                }

                String fileName = fileService.uploadFile(logoFile, "shop-logos");
                String fileUrl = fileService.getFileUrl(fileName);
                request.setLogoUrl(fileUrl);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Lỗi khi upload logo: " + e.getMessage());
                redirectAttributes.addFlashAttribute("sellerRequestRequest", request);
                return "redirect:/seller/become-seller";
            }
        }

        // Handle cover image file upload
        if (coverImageFile != null && !coverImageFile.isEmpty()) {
            try {
                String contentType = coverImageFile.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    redirectAttributes.addFlashAttribute("error", "Ảnh bìa phải là file ảnh");
                    redirectAttributes.addFlashAttribute("sellerRequestRequest", request);
                    return "redirect:/seller/become-seller";
                }

                if (coverImageFile.getSize() > 25 * 1024 * 1024) {
                    redirectAttributes.addFlashAttribute("error", "Kích thước ảnh bìa không được vượt quá 25MB");
                    redirectAttributes.addFlashAttribute("sellerRequestRequest", request);
                    return "redirect:/seller/become-seller";
                }

                String fileName = fileService.uploadFile(coverImageFile, "shop-covers");
                String fileUrl = fileService.getFileUrl(fileName);
                request.setCoverImageUrl(fileUrl);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Lỗi khi upload ảnh bìa: " + e.getMessage());
                redirectAttributes.addFlashAttribute("sellerRequestRequest", request);
                return "redirect:/seller/become-seller";
            }
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.sellerRequestRequest", bindingResult);
            redirectAttributes.addFlashAttribute("sellerRequestRequest", request);
            redirectAttributes.addFlashAttribute("error", "Vui lòng kiểm tra lại thông tin");
            return "redirect:/seller/become-seller";
        }

        try {
            sellerRequestService.createRequest(user.getId(), request);
            redirectAttributes.addFlashAttribute("success", "Gửi yêu cầu thành công. Vui lòng chờ admin duyệt.");
            return "redirect:/seller/become-seller";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("sellerRequestRequest", request);
            return "redirect:/seller/become-seller";
        }
    }

    @PostMapping("/become-seller/update")
    public String updateSellerRequest(
            @CurrentUser User user,
            @Valid @ModelAttribute("sellerRequestRequest") SellerRequestRequest request,
            @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
            @RequestParam(value = "coverImageFile", required = false) MultipartFile coverImageFile,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        
        if (user == null) {
            return "redirect:/login";
        }

        // Handle logo file upload
        if (logoFile != null && !logoFile.isEmpty()) {
            try {
                String contentType = logoFile.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    redirectAttributes.addFlashAttribute("error", "Logo phải là file ảnh");
                    redirectAttributes.addFlashAttribute("sellerRequestRequest", request);
                    return "redirect:/seller/become-seller";
                }

                if (logoFile.getSize() > 25 * 1024 * 1024) {
                    redirectAttributes.addFlashAttribute("error", "Kích thước logo không được vượt quá 25MB");
                    redirectAttributes.addFlashAttribute("sellerRequestRequest", request);
                    return "redirect:/seller/become-seller";
                }

                String fileName = fileService.uploadFile(logoFile, "shop-logos");
                String fileUrl = fileService.getFileUrl(fileName);
                request.setLogoUrl(fileUrl);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Lỗi khi upload logo: " + e.getMessage());
                redirectAttributes.addFlashAttribute("sellerRequestRequest", request);
                return "redirect:/seller/become-seller";
            }
        }

        // Handle cover image file upload
        if (coverImageFile != null && !coverImageFile.isEmpty()) {
            try {
                String contentType = coverImageFile.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    redirectAttributes.addFlashAttribute("error", "Ảnh bìa phải là file ảnh");
                    redirectAttributes.addFlashAttribute("sellerRequestRequest", request);
                    return "redirect:/seller/become-seller";
                }

                if (coverImageFile.getSize() > 25 * 1024 * 1024) {
                    redirectAttributes.addFlashAttribute("error", "Kích thước ảnh bìa không được vượt quá 25MB");
                    redirectAttributes.addFlashAttribute("sellerRequestRequest", request);
                    return "redirect:/seller/become-seller";
                }

                String fileName = fileService.uploadFile(coverImageFile, "shop-covers");
                String fileUrl = fileService.getFileUrl(fileName);
                request.setCoverImageUrl(fileUrl);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Lỗi khi upload ảnh bìa: " + e.getMessage());
                redirectAttributes.addFlashAttribute("sellerRequestRequest", request);
                return "redirect:/seller/become-seller";
            }
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.sellerRequestRequest", bindingResult);
            redirectAttributes.addFlashAttribute("sellerRequestRequest", request);
            redirectAttributes.addFlashAttribute("error", "Vui lòng kiểm tra lại thông tin");
            return "redirect:/seller/become-seller";
        }

        try {
            sellerRequestService.updateRequest(user.getId(), request);
            redirectAttributes.addFlashAttribute("success", "Cập nhật yêu cầu thành công");
            return "redirect:/seller/become-seller";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("sellerRequestRequest", request);
            return "redirect:/seller/become-seller";
        }
    }

    @PostMapping("/become-seller/cancel")
    public String cancelSellerRequest(
            @CurrentUser User user,
            RedirectAttributes redirectAttributes) {
        
        if (user == null) {
            return "redirect:/login";
        }

        try {
            sellerRequestService.cancelRequest(user.getId());
            redirectAttributes.addFlashAttribute("success", "Đã hủy yêu cầu thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/seller/become-seller";
    }

    @GetMapping("/shop")
    public String showShop(@CurrentUser User user, Model model) {
        if (user == null) {
            return "redirect:/login";
        }

        try {
            ShopResponse shop = shopService.getShopByUserId(user.getId());
            model.addAttribute("shop", shop);
            
            if (!model.containsAttribute("updateShopRequest")) {
                UpdateShopRequest updateRequest = new UpdateShopRequest();
                updateRequest.setName(shop.getName());
                updateRequest.setDescription(shop.getDescription());
                updateRequest.setPhoneNumber(shop.getPhoneNumber());
                updateRequest.setAddress(shop.getAddress());
                updateRequest.setDistrictId(shop.getDistrictId());
                updateRequest.setWardCode(shop.getWardCode());
                updateRequest.setLogoUrl(shop.getLogoUrl());
                updateRequest.setCoverImageUrl(shop.getCoverImageUrl());
                model.addAttribute("updateShopRequest", updateRequest);
            }
        } catch (Exception e) {
            model.addAttribute("error", "Bạn chưa có shop. Vui lòng đăng ký làm seller trước.");
            return "redirect:/seller/become-seller";
        }
        
        return "seller/shop";
    }

    @PostMapping("/shop")
    public String updateShop(
            @CurrentUser User user,
            @Valid @ModelAttribute("updateShopRequest") UpdateShopRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        
        if (user == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.updateShopRequest", bindingResult);
            redirectAttributes.addFlashAttribute("updateShopRequest", request);
            redirectAttributes.addFlashAttribute("error", "Vui lòng kiểm tra lại thông tin");
            return "redirect:/seller/shop";
        }

        try {
            shopService.updateShop(user.getId(), request);
            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin shop thành công");
            return "redirect:/seller/shop";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("updateShopRequest", request);
            return "redirect:/seller/shop";
        }
    }
}
