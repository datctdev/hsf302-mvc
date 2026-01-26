package com.hsf.e_comerce.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public String handleEmailAlreadyExistsException(
            EmailAlreadyExistsException ex, 
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/register";
    }

    @ExceptionHandler({InvalidCredentialsException.class, BadCredentialsException.class})
    public String handleInvalidCredentialsException(
            Exception ex, 
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "Email hoặc mật khẩu không đúng");
        return "redirect:/login";
    }

    @ExceptionHandler({UserNotFoundException.class, UsernameNotFoundException.class})
    public String handleUserNotFoundException(
            Exception ex, 
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleValidationExceptions(
            MethodArgumentNotValidException ex, 
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        redirectAttributes.addFlashAttribute("errors", errors);
        redirectAttributes.addFlashAttribute("error", "Dữ liệu đầu vào không hợp lệ");
        
        // Redirect về trang trước đó hoặc trang chủ
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null && !referer.isEmpty() ? referer : "/");
    }

    @ExceptionHandler(CustomException.class)
    public String handleCustomException(
            CustomException ex, 
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/";
    }

    @ExceptionHandler(Exception.class)
    public String handleGlobalException(
            Exception ex, 
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        // Log error for debugging
        ex.printStackTrace();
        
        redirectAttributes.addFlashAttribute("error", "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.");
        
        // Redirect về trang trước đó hoặc trang chủ
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null && !referer.isEmpty() ? referer : "/");
    }
}
