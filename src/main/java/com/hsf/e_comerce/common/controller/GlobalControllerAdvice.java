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
    
    // Sentinel value to represent null in cache (since ConcurrentHashMap doesn't allow null values)
    private static final Object NULL_USER = new Object();

    @ModelAttribute("currentUser")
    public User getCurrentUser() {
        // Check cache first
        ConcurrentHashMap<String, Object> cache = REQUEST_CACHE.get();
        if (cache == null) {
            cache = new ConcurrentHashMap<>();
            REQUEST_CACHE.set(cache);
        }
        
        String cacheKey = "currentUser";
        Object cached = cache.get(cacheKey);
        if (cached != null) {
            // Use sentinel value NULL_USER to represent null
            if (cached == NULL_USER) {
                return null;
            }
            return (User) cached;
        }
        
        User result = null;
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
                String username = auth.getName();
                if (username != null && !username.equals("anonymousUser") && !username.isEmpty()) {
                    try {
                        // Use optimized method that fetches role in same query
                        result = userService.findByEmailWithRole(username);
                    } catch (Exception e) {
                        // User not found, return null silently
                        result = null;
                    }
                }
            }
        } catch (Exception e) {
            // Any error, return null silently
            result = null;
        }
        
        // Ensure cache is still valid before putting
        ConcurrentHashMap<String, Object> finalCache = REQUEST_CACHE.get();
        if (finalCache == null) {
            finalCache = new ConcurrentHashMap<>();
            REQUEST_CACHE.set(finalCache);
        }
        // Use sentinel value for null since ConcurrentHashMap doesn't allow null values
        finalCache.put(cacheKey, result != null ? result : NULL_USER);
        return result;
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
        
        Integer result = 0;
        try {
            User currentUser = getCurrentUser(); // This will use cached value if available
            if (currentUser != null) {
                result = cartService.getCartItemCount(currentUser);
            }
        } catch (Exception e) {
            // Any error, return 0 silently
            result = 0;
        }
        
        // Ensure cache is still valid before putting
        ConcurrentHashMap<String, Object> finalCache = REQUEST_CACHE.get();
        if (finalCache == null) {
            finalCache = new ConcurrentHashMap<>();
            REQUEST_CACHE.set(finalCache);
        }
        finalCache.put(cacheKey, result);
        return result;
    }
    
    /**
     * Clean up ThreadLocal after request to prevent memory leak.
     * Called by RequestCacheCleanupFilter.
     */
    public static void cleanupRequestCache() {
        REQUEST_CACHE.remove();
    }
}
