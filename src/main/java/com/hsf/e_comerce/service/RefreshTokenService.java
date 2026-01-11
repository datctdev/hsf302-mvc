package com.hsf.e_comerce.service;

import com.hsf.e_comerce.entity.RefreshToken;
import com.hsf.e_comerce.entity.User;

public interface RefreshTokenService {
    
    RefreshToken createRefreshToken(User user);
    
    RefreshToken verifyRefreshToken(String token);
    
    void revokeRefreshToken(String token);
    
    void revokeAllUserTokens(User user);
    
    void deleteExpiredTokens();
}
