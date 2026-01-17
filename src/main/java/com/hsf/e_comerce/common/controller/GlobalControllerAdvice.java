package com.hsf.e_comerce.common.controller;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.auth.service.UserService;
import com.hsf.e_comerce.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.concurrent.ConcurrentHashMap;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final UserService userService;
    private final CartService cartService;
    
    // Request-scoped cache using ThreadLocal to avoid multiple queries per request
    private static final ThreadLocal<ConcurrentHashMap<String, Object>> REQUEST_CACHE = new ThreadLocal<>();

    @ModelAttribute("currentUser")
    public User getCurrentUser() {
        // Check cache first
        ConcurrentHashMap<String, Object> cache = REQUEST_CACHE.get();
        if (cache == null) {
            cache = new ConcurrentHashMap<>();
            REQUEST_CACHE.set(cache);
        }
        
        String cacheKey = "currentUser";
        if (cache.containsKey(cacheKey)) {
            return (User) cache.get(cacheKey);
        }
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
                String username = auth.getName();
                if (username != null && !username.equals("anonymousUser") && !username.isEmpty()) {
                    try {
                        // Use optimized method that fetches role in same query
                        User user = userService.findByEmailWithRole(username);
                        cache.put(cacheKey, user);
                        return user;
                    } catch (Exception e) {
                        // User not found, return null silently
                        cache.put(cacheKey, null);
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            // Any error, return null silently
            cache.put(cacheKey, null);
            return null;
        }
        
        cache.put(cacheKey, null);
        return null;
    }

    @ModelAttribute("cartItemCount")
    public Integer getCartItemCount() {
        // Check cache first
        ConcurrentHashMap<String, Object> cache = REQUEST_CACHE.get();
        if (cache == null) {
            cache = new ConcurrentHashMap<>();
            REQUEST_CACHE.set(cache);
        }
        
        String cacheKey = "cartItemCount";
        if (cache.containsKey(cacheKey)) {
            return (Integer) cache.get(cacheKey);
        }
        
        try {
            User currentUser = getCurrentUser(); // This will use cached value if available
            if (currentUser != null) {
                Integer count = cartService.getCartItemCount(currentUser);
                cache.put(cacheKey, count);
                return count;
            }
        } catch (Exception e) {
            // Any error, return 0 silently
        }
        
        cache.put(cacheKey, 0);
        return 0;
    }
    
    /**
     * Clean up ThreadLocal after request to prevent memory leak.
     * Called by RequestCacheCleanupFilter.
     */
    public static void cleanupRequestCache() {
        REQUEST_CACHE.remove();
    }
}
