package com.hsf.e_comerce.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
@Slf4j
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        
        // Check if user is admin
        boolean isAdmin = authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        // Check if user is seller
        boolean isSeller = authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_SELLER"));
        
        // Redirect based on role
        if (isAdmin) {
            response.sendRedirect("/admin/dashboard");
        } else if (isSeller) {
            response.sendRedirect("/seller/products");
        } else {
            // Default redirect for buyers
            response.sendRedirect("/");
        }
    }
}
