package com.hsf.e_comerce.common.filter;

import jakarta.servlet.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
public class RequestCacheCleanupFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } finally {
            // Cleanup ThreadLocal after request to prevent memory leak
            com.hsf.e_comerce.common.controller.GlobalControllerAdvice.cleanupRequestCache();
        }
    }
}
