package com.hsf.e_comerce.auth.service.impl;

import com.hsf.e_comerce.auth.entity.RefreshToken;
import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.auth.repository.RefreshTokenRepository;
import com.hsf.e_comerce.auth.service.JwtService;
import com.hsf.e_comerce.auth.service.RefreshTokenService;
import com.hsf.e_comerce.common.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Xóa refresh token cũ của user (nếu có)
        refreshTokenRepository.findByUserId(user.getId())
                .ifPresent(refreshTokenRepository::delete);

        // Tạo refresh token mới
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateRefreshToken(userDetails);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7)); // 7 ngày
        refreshToken.setIsRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidCredentialsException("Refresh token không hợp lệ"));

        // Kiểm tra token đã bị revoke chưa
        if (refreshToken.getIsRevoked()) {
            throw new InvalidCredentialsException("Refresh token đã bị thu hồi");
        }

        // Kiểm tra token đã hết hạn chưa
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidCredentialsException("Refresh token đã hết hạn");
        }

        // Kiểm tra JWT token có hợp lệ không
        UserDetails userDetails = userDetailsService.loadUserByUsername(refreshToken.getUser().getEmail());
        if (jwtService.isTokenExpired(token)) {
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidCredentialsException("Refresh token đã hết hạn");
        }

        return refreshToken;
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshToken -> {
                    refreshToken.setIsRevoked(true);
                    refreshTokenRepository.save(refreshToken);
                });
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllUserTokens(user.getId());
    }

    @Override
    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}
