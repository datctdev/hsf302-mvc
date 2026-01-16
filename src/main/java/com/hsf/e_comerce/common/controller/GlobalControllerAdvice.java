package com.hsf.e_comerce.common.controller;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final UserService userService;

    @ModelAttribute("currentUser")
    public User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
                String username = auth.getName();
                if (username != null && !username.equals("anonymousUser") && !username.isEmpty()) {
                    try {
                        return userService.findByEmail(username);
                    } catch (Exception e) {
                        // User not found, return null silently
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            // Any error, return null silently
            return null;
        }
        return null;
    }
}
