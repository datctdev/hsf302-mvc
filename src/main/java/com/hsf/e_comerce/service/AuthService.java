package com.hsf.e_comerce.service;

import com.hsf.e_comerce.dto.request.ChangePasswordRequest;
import com.hsf.e_comerce.dto.request.LoginRequest;
import com.hsf.e_comerce.dto.request.RefreshTokenRequest;
import com.hsf.e_comerce.dto.request.RegisterRequest;
import com.hsf.e_comerce.dto.request.UpdateProfileRequest;
import com.hsf.e_comerce.dto.response.AuthResponse;
import com.hsf.e_comerce.dto.response.UserResponse;

import java.util.UUID;

public interface AuthService {
    
    AuthResponse register(RegisterRequest request);
    
    AuthResponse login(LoginRequest request);
    
    AuthResponse refreshToken(RefreshTokenRequest request);
    
    void changePassword(UUID userId, ChangePasswordRequest request);
    
    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);
    
    void deactivateAccount(UUID userId);
    
    void activateAccount(UUID userId);
}
