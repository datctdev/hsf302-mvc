package com.hsf.e_comerce.auth.controller;

import com.hsf.e_comerce.auth.dto.request.ChangePasswordRequest;
import com.hsf.e_comerce.auth.dto.request.LoginRequest;
import com.hsf.e_comerce.auth.dto.request.RefreshTokenRequest;
import com.hsf.e_comerce.auth.dto.request.RegisterRequest;
import com.hsf.e_comerce.auth.dto.request.UpdateProfileRequest;
import com.hsf.e_comerce.auth.dto.response.AuthResponse;
import com.hsf.e_comerce.auth.dto.response.UserResponse;
import com.hsf.e_comerce.auth.service.AuthService;
import com.hsf.e_comerce.auth.service.UserService;
import com.hsf.e_comerce.common.dto.response.MessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        // JWT là stateless, logout chỉ cần client xóa token
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Đăng xuất thành công")
                .build());
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        var user = userService.findByEmail(email);
        var roles = userService.getUserRoles(user.getId());

        UserResponse response = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .roles(roles)
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        String email = authentication.getName();
        var user = userService.findByEmail(email);
        authService.changePassword(user.getId(), request);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Mật khẩu đã được thay đổi thành công")
                .build());
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        String email = authentication.getName();
        var user = userService.findByEmail(email);
        UserResponse response = authService.updateProfile(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/deactivate")
    public ResponseEntity<MessageResponse> deactivateAccount(Authentication authentication) {
        String email = authentication.getName();
        var user = userService.findByEmail(email);
        authService.deactivateAccount(user.getId());
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Tài khoản đã được vô hiệu hóa")
                .build());
    }

    @PostMapping("/activate")
    public ResponseEntity<MessageResponse> activateAccount(Authentication authentication) {
        String email = authentication.getName();
        var user = userService.findByEmail(email);
        authService.activateAccount(user.getId());
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Tài khoản đã được kích hoạt")
                .build());
    }
}
