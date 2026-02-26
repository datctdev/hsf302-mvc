package com.hsf.e_comerce.platform.controller;

import com.hsf.e_comerce.platform.dto.request.CommissionFilterRequest;
import com.hsf.e_comerce.platform.dto.request.UpdateCommissionRateRequest;
import com.hsf.e_comerce.platform.service.CommissionService;
import com.hsf.e_comerce.platform.service.PlatformSettingService;
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
import java.util.UUID;

@Controller
@RequestMapping("/admin/commission")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommissionMvcController {

    private final PlatformSettingService platformSettingService;
    private final CommissionService commissionService;
    private final ShopService shopService;

    @GetMapping
    public String commissionForm(Model model) {
        BigDecimal currentRate = platformSettingService.getCommissionRate();
        model.addAttribute("commissionRate", currentRate);
        model.addAttribute("updateCommissionRateRequest",
                UpdateCommissionRateRequest.builder().commissionRate(currentRate).build());
        model.addAttribute("shops", shopService.getAllShop());
        return "admin/commission";
    }

    @PostMapping
    public String updateCommissionRate(
            @Valid @ModelAttribute("updateCommissionRateRequest") UpdateCommissionRateRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error",
                    bindingResult.getFieldError("commissionRate") != null
                            ? bindingResult.getFieldError("commissionRate").getDefaultMessage()
                            : "Vui lòng nhập tỷ lệ hoa hồng từ 0 đến 100.");
            return "redirect:/admin/commission";
        }

        try {
            platformSettingService.setCommissionRate(request.getCommissionRate());
            redirectAttributes.addFlashAttribute("success",
                    "Đã cập nhật tỷ lệ hoa hồng thành " + request.getCommissionRate().stripTrailingZeros().toPlainString() + "%.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/commission";
    }

    // GET /admin/commission/orders
    @GetMapping("/orders")
    public String list(CommissionFilterRequest filter, Model model) {
        model.addAttribute("commissions", commissionService.getCommissions(filter));
        return "admin/commission-history";
    }

    // GET /admin/commission/orders/{orderId}
    @GetMapping("/orders/{orderId}")
    public String detail(@PathVariable UUID orderId, Model model) {
        model.addAttribute("commission", commissionService.getByOrderId(orderId));
        return "admin/commission-detail";
    }
}
