package com.hsf.e_comerce.auth.controller;

import com.hsf.e_comerce.auth.dto.request.ChangePasswordRequest;
import com.hsf.e_comerce.auth.dto.request.RegisterRequest;
import com.hsf.e_comerce.auth.dto.request.UpdateProfileRequest;
import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.auth.service.AuthService;
import com.hsf.e_comerce.auth.service.UserService;
import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.file.service.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthMvcController {

    private final AuthService authService;
    private final UserService userService;
    private final FileService fileService;

    @GetMapping("/verify-email")
    public String verifyEmail(
            @RequestParam String token,
            RedirectAttributes redirectAttributes) {
        try {
            authService.verifyEmail(token);
            redirectAttributes.addFlashAttribute("success", "Email đã được xác minh. Vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/resend-verification")
    public String showResendVerificationForm(Model model) {
        return "auth/resend-verification";
    }

    @PostMapping("/resend-verification")
    public String resendVerification(
            @RequestParam String email,
            RedirectAttributes redirectAttributes) {
        try {
            authService.resendVerificationEmail(email.trim());
            redirectAttributes.addFlashAttribute("success", "Đã gửi lại email xác minh. Vui lòng kiểm tra hộp thư.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addAttribute("email", email);
            return "redirect:/resend-verification";
        }
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.registerRequest", bindingResult);
            redirectAttributes.addFlashAttribute("registerRequest", request);
            redirectAttributes.addFlashAttribute("error", "Vui lòng kiểm tra lại thông tin đăng ký");
            return "redirect:/register";
        }

        try {
            authService.register(request);
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Vui lòng kiểm tra email để xác minh tài khoản trước khi đăng nhập.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("registerRequest", request);
            return "redirect:/register";
        }
    }

    @GetMapping("/profile")
    public String showProfile(@CurrentUser User user, Model model) {
        if (user == null) {
            return "redirect:/login";
        }
        
        com.hsf.e_comerce.auth.dto.response.UserResponse userResponse = 
            com.hsf.e_comerce.auth.dto.response.UserResponse.convertToResponse(user, userService);
        model.addAttribute("user", userResponse);
        
        if (!model.containsAttribute("updateProfileRequest")) {
            UpdateProfileRequest updateRequest = new UpdateProfileRequest();
            updateRequest.setFullName(user.getFullName());
            updateRequest.setPhoneNumber(user.getPhoneNumber());
            updateRequest.setAvatarUrl(user.getAvatarUrl());
            model.addAttribute("updateProfileRequest", updateRequest);
        }
        
        return "auth/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @CurrentUser User user,
            @Valid @ModelAttribute("updateProfileRequest") UpdateProfileRequest request,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        
        if (user == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.updateProfileRequest", bindingResult);
            redirectAttributes.addFlashAttribute("updateProfileRequest", request);
            redirectAttributes.addFlashAttribute("error", "Vui lòng kiểm tra lại thông tin");
            return "redirect:/profile";
        }

        try {
            // Xử lý upload avatar file nếu có
            if (avatarFile != null && !avatarFile.isEmpty()) {
                // Validate file type
                String contentType = avatarFile.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    redirectAttributes.addFlashAttribute("error", "Chỉ chấp nhận file ảnh");
                    redirectAttributes.addFlashAttribute("updateProfileRequest", request);
                    return "redirect:/profile";
                }

                // Validate file size (25MB)
                if (avatarFile.getSize() > 25 * 1024 * 1024) {
                    redirectAttributes.addFlashAttribute("error", "Kích thước file không được vượt quá 25MB");
                    redirectAttributes.addFlashAttribute("updateProfileRequest", request);
                    return "redirect:/profile";
                }

                // Upload file to MinIO
                String fileName = fileService.uploadFile(avatarFile, "avatars");
                String fileUrl = fileService.getFileUrl(fileName);
                
                // Set avatarUrl from uploaded file
                request.setAvatarUrl(fileUrl);
            }

            authService.updateProfile(user.getId(), request);
            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công");
            return "redirect:/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("updateProfileRequest", request);
            return "redirect:/profile";
        }
    }

    @GetMapping("/change-password")
    public String showChangePasswordForm(Model model) {
        if (!model.containsAttribute("changePasswordRequest")) {
            model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        }
        return "auth/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(
            @CurrentUser User user,
            @Valid @ModelAttribute("changePasswordRequest") ChangePasswordRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        
        if (user == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.changePasswordRequest", bindingResult);
            redirectAttributes.addFlashAttribute("changePasswordRequest", request);
            redirectAttributes.addFlashAttribute("error", "Vui lòng kiểm tra lại thông tin");
            return "redirect:/change-password";
        }

        try {
            authService.changePassword(user.getId(), request);
            redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công");
            return "redirect:/change-password";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("changePasswordRequest", request);
            return "redirect:/change-password";
        }
    }

    @PostMapping("/activate")
    public String activateAccount(
            @CurrentUser User user,
            RedirectAttributes redirectAttributes) {
        
        if (user == null) {
            return "redirect:/login";
        }

        try {
            authService.activateAccount(user.getId());
            redirectAttributes.addFlashAttribute("success", "Tài khoản đã được kích hoạt");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/profile";
    }

    @PostMapping("/deactivate")
    public String deactivateAccount(
            @CurrentUser User user,
            RedirectAttributes redirectAttributes) {
        
        if (user == null) {
            return "redirect:/login";
        }

        try {
            authService.deactivateAccount(user.getId());
            SecurityContextHolder.clearContext();
            redirectAttributes.addFlashAttribute("success", "Tài khoản đã được vô hiệu hóa");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/profile";
        }
    }
}
