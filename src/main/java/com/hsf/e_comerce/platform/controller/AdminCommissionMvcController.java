package com.hsf.e_comerce.platform.controller;

import com.hsf.e_comerce.platform.dto.request.UpdateCommissionRateRequest;
import com.hsf.e_comerce.platform.service.PlatformSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/commission")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommissionMvcController {

    private final PlatformSettingService platformSettingService;

    @GetMapping
    public String commissionForm(Model model) {
        BigDecimal currentRate = platformSettingService.getCommissionRate();
        model.addAttribute("commissionRate", currentRate);
        model.addAttribute("updateCommissionRateRequest",
                UpdateCommissionRateRequest.builder().commissionRate(currentRate).build());
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
}
