package com.hsf.e_comerce.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        String requestPath = request.getRequestURI();
        
        // Nếu là API request, trả về JSON
        if (requestPath.startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"error\":\"Unauthorized\",\"message\":\"Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.\"}"
            );
        } else {
            // Nếu là HTML request, redirect về trang login
            response.sendRedirect("/login?expired=true");
        }
    }
}
