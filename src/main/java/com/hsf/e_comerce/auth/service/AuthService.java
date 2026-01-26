package com.hsf.e_comerce.auth.service;

import com.hsf.e_comerce.auth.dto.request.ChangePasswordRequest;
import com.hsf.e_comerce.auth.dto.request.LoginRequest;
import com.hsf.e_comerce.auth.dto.request.RefreshTokenRequest;
import com.hsf.e_comerce.auth.dto.request.RegisterRequest;
import com.hsf.e_comerce.auth.dto.request.UpdateProfileRequest;
import com.hsf.e_comerce.auth.dto.response.AuthResponse;
import com.hsf.e_comerce.auth.dto.response.UserResponse;

import java.util.UUID;

public interface AuthService {
    
    AuthResponse register(RegisterRequest request);
    
    void verifyEmail(String token);
    
    void resendVerificationEmail(String email);
    
    AuthResponse login(LoginRequest request);
    
    AuthResponse refreshToken(RefreshTokenRequest request);
    
    void changePassword(UUID userId, ChangePasswordRequest request);
    
    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);
    
    void deactivateAccount(UUID userId);
    
    void activateAccount(UUID userId);
}
