package com.hsf.e_comerce.config;

import com.hsf.e_comerce.common.exception.EmailNotVerifiedException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        boolean emailNotVerified = exception instanceof EmailNotVerifiedException
                || (exception.getCause() != null && exception.getCause() instanceof EmailNotVerifiedException);
        if (emailNotVerified) {
            setDefaultFailureUrl("/login?error=email_not_verified");
        } else {
            setDefaultFailureUrl("/login?error=true");
        }
        super.onAuthenticationFailure(request, response, exception);
    }
}
