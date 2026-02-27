package com.hsf.e_comerce.common.resolver;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.auth.service.UserService;
import com.hsf.e_comerce.common.annotation.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserService userService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) 
                && parameter.getParameterType().equals(User.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {

        Authentication authentication = (Authentication) webRequest.getUserPrincipal();

        // Cho phép xem trang công khai (vd. chi tiết sản phẩm) khi chưa đăng nhập → trả null
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        if (!(authentication.getPrincipal() instanceof UserDetails)) {
            return null;
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        if (email == null || email.isEmpty() || "anonymousUser".equals(email)) {
            return null;
        }

        // Đã đăng nhập thì bắt buộc resolve được user (tránh null → lỗi khi thêm giỏ, v.v.)
        return userService.findByEmail(email);
    }
}
