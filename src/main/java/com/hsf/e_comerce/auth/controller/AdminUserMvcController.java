package com.hsf.e_comerce.auth.controller;

import com.hsf.e_comerce.auth.dto.response.UserResponse;
import com.hsf.e_comerce.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserMvcController {

    private final UserService userService;

    @GetMapping
    public String getAllUsers(Model model) {
        List<UserResponse> users = userService.getAllUserResponses();
        model.addAttribute("users", users);
        return "admin/users";
    }

    @GetMapping("/edit/{id}")
    public String showEditUserForm(@PathVariable UUID id, Model model) {
        try {
            UserResponse user = userService.getUserResponseById(id);
            model.addAttribute("user", user);
            return "admin/user-edit";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/admin/users";
        }
    }

    @PostMapping("/edit/{id}")
    public String updateUser(
            @PathVariable UUID id,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) Boolean isActive,
            RedirectAttributes redirectAttributes) {
        
        try {
            userService.updateUserAndGetResponse(id, fullName, email, phoneNumber, roleName, isActive);
            redirectAttributes.addFlashAttribute("success", "Cập nhật user thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/activate")
    public String activateUser(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes) {
        
        try {
            userService.activateUser(id);
            redirectAttributes.addFlashAttribute("success", "Kích hoạt user thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivateUser(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes) {
        
        try {
            userService.deactivateUser(id);
            redirectAttributes.addFlashAttribute("success", "Vô hiệu hóa user thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/admin/users";
    }
}
