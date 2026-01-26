package com.hsf.e_comerce.auth.service.impl;

import com.hsf.e_comerce.auth.dto.request.ChangePasswordRequest;
import com.hsf.e_comerce.auth.dto.request.LoginRequest;
import com.hsf.e_comerce.auth.dto.request.RefreshTokenRequest;
import com.hsf.e_comerce.auth.dto.request.RegisterRequest;
import com.hsf.e_comerce.auth.dto.request.UpdateProfileRequest;
import com.hsf.e_comerce.auth.dto.response.AuthResponse;
import com.hsf.e_comerce.auth.dto.response.UserResponse;
import com.hsf.e_comerce.auth.entity.EmailVerificationToken;
import com.hsf.e_comerce.auth.entity.RefreshToken;
import com.hsf.e_comerce.auth.entity.Role;
import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.auth.repository.EmailVerificationTokenRepository;
import com.hsf.e_comerce.auth.repository.RoleRepository;
import com.hsf.e_comerce.auth.repository.UserRepository;
import com.hsf.e_comerce.auth.service.AuthService;
import com.hsf.e_comerce.auth.service.EmailService;
import com.hsf.e_comerce.auth.service.JwtService;
import com.hsf.e_comerce.auth.service.RefreshTokenService;
import com.hsf.e_comerce.auth.service.UserService;
import com.hsf.e_comerce.common.exception.EmailAlreadyExistsException;
import com.hsf.e_comerce.common.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailService emailService;
    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.email-verification-expiry-hours:24}")
    private int verificationExpiryHours;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Kiểm tra email đã tồn tại chưa
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        // Tạo user mới (chưa xác minh email)
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setIsActive(true);
        user.setEmailVerified(false);

        // Gán role mặc định ROLE_BUYER TRƯỚC KHI save
        Role buyerRole = roleRepository.findByName("ROLE_BUYER")
                .orElseThrow(() -> new RuntimeException("Role ROLE_BUYER not found"));
        user.setRole(buyerRole);

        // Lưu user vào database (sau khi đã có role)
        user = userRepository.save(user);

        // Tạo token xác minh và gửi email
        String token = UUID.randomUUID().toString();
        EmailVerificationToken evToken = new EmailVerificationToken();
        evToken.setUser(user);
        evToken.setToken(token);
        evToken.setExpiresAt(LocalDateTime.now().plusHours(verificationExpiryHours));
        emailVerificationTokenRepository.save(evToken);

        String verificationLink = "/verify-email?token=" + token;
        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), verificationLink);

        // Lấy roles của user
        var roles = userService.getUserRoles(user.getId());

        // MVC không cần JWT token, chỉ trả về thông tin user
        return AuthResponse.builder()
                .token(null) // Không tạo token cho MVC
                .type(null)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roles)
                .expiresAt(null)
                .build();
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken evToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Link xác minh không hợp lệ hoặc đã hết hạn."));
        if (evToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            emailVerificationTokenRepository.delete(evToken);
            throw new IllegalArgumentException("Link xác minh đã hết hạn. Vui lòng yêu cầu gửi lại email.");
        }
        User user = evToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        emailVerificationTokenRepository.delete(evToken);
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new IllegalArgumentException("Email chưa đăng ký. Vui lòng đăng ký tài khoản."));
        if (user.getEmailVerified()) {
            throw new IllegalArgumentException("Email đã được xác minh. Bạn có thể đăng nhập.");
        }
        // Xóa token cũ nếu có
        emailVerificationTokenRepository.findByUser(user).ifPresent(emailVerificationTokenRepository::delete);

        String token = UUID.randomUUID().toString();
        EmailVerificationToken evToken = new EmailVerificationToken();
        evToken.setUser(user);
        evToken.setToken(token);
        evToken.setExpiresAt(LocalDateTime.now().plusHours(verificationExpiryHours));
        emailVerificationTokenRepository.save(evToken);

        String verificationLink = "/verify-email?token=" + token;
        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), verificationLink);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            // Xác thực credentials
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (Exception e) {
            throw new InvalidCredentialsException();
        }

        // Load user
        User user = userService.findByEmail(request.getEmail());

        // Kiểm tra tài khoản có active không
        if (!user.getIsActive()) {
            throw new InvalidCredentialsException();
        }

        // Lấy roles của user
        var roles = userService.getUserRoles(user.getId());

        // MVC không cần JWT token, Spring Security sẽ xử lý authentication
        // Login được xử lý bởi Spring Security form login
        return AuthResponse.builder()
                .token(null) // Không tạo token cho MVC
                .type(null)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roles)
                .expiresAt(null)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        // Verify refresh token
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(request.getRefreshToken());
        User user = refreshToken.getUser();

        // Kiểm tra tài khoản có active không
        if (!user.getIsActive()) {
            throw new InvalidCredentialsException();
        }

        // Load user details
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        // Tạo access token mới
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId().toString());
        extraClaims.put("roles", userService.getUserRoles(user.getId()));

        String newAccessToken = jwtService.generateToken(extraClaims, userDetails);

        // Lấy roles
        var roles = userService.getUserRoles(user.getId());

        // Tính toán thời gian hết hạn
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        return AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(refreshToken.getToken())
                .type("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roles)
                .expiresAt(expiresAt)
                .build();
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        // Validate new password và confirm password
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        // Load user
        User user = userService.findById(userId);

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Mật khẩu hiện tại không đúng");
        }

        // Check new password is different from current
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        // MVC không cần revoke refresh tokens
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userService.findById(userId);

        if (request.getFullName() != null && !request.getFullName().isEmpty()) {
            user.setFullName(request.getFullName());
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        user = userRepository.save(user);
        return UserResponse.convertToResponse(user, userService);
    }

    @Override
    @Transactional
    public void deactivateAccount(UUID userId) {
        User user = userService.findById(userId);
        user.setIsActive(false);
        userRepository.save(user);
        
        // MVC không cần revoke refresh tokens
    }

    @Override
    @Transactional
    public void activateAccount(UUID userId) {
        User user = userService.findById(userId);
        user.setIsActive(true);
        userRepository.save(user);
    }
}
