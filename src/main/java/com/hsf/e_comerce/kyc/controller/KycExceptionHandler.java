package com.hsf.e_comerce.kyc.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
@Slf4j
public class KycExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        
        log.error("=== MethodArgumentTypeMismatchException ===");
        log.error("Request URL: {}", request.getRequestURL());
        log.error("Request Method: {}", request.getMethod());
        log.error("Query String: {}", request.getQueryString());
        log.error("Parameter Name: {}", ex.getName());
        log.error("Parameter Value: {}", ex.getValue());
        log.error("Required Type: {}", ex.getRequiredType());
        log.error("Exception Message: {}", ex.getMessage());
        
        // Log all request parameters
        log.error("All Parameters:");
        request.getParameterMap().forEach((key, value) -> 
            log.error("  {} = {}", key, String.join(",", value))
        );
        
        // Log path variables from URI
        log.error("Request URI: {}", request.getRequestURI());
        
        redirectAttributes.addFlashAttribute("error", 
            "Lỗi xử lý yêu cầu. Vui lòng thử lại.");
        return "redirect:/kyc";
    }
}
