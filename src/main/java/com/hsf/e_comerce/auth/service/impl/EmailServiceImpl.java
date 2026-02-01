package com.hsf.e_comerce.auth.service.impl;

import com.hsf.e_comerce.auth.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String emailFrom;

    @Override
    public void sendVerificationEmail(String toEmail, String fullName, String verificationLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(emailFrom, "E-commerce");
            helper.setTo(toEmail);
            helper.setSubject("Xác minh email - E-commerce");

            String fullLink = verificationLink.startsWith("http") ? verificationLink : baseUrl + verificationLink;
            String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2 style="color: #333;">Xác minh địa chỉ email</h2>
                    <p>Xin chào <strong>%s</strong>,</p>
                    <p>Bạn vừa đăng ký tài khoản trên E-commerce. Vui lòng nhấn vào nút bên dưới để xác minh email của bạn:</p>
                    <p style="margin: 24px 0;">
                        <a href="%s" style="background: #007bff; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; display: inline-block;">Xác minh email</a>
                    </p>
                    <p style="color: #666; font-size: 14px;">Link có hiệu lực trong 24 giờ. Nếu bạn không đăng ký, vui lòng bỏ qua email này.</p>
                    <p style="color: #666; font-size: 14px;">Hoặc copy link: %s</p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;">
                    <p style="color: #999; font-size: 12px;">E-commerce</p>
                </div>
                """.formatted(fullName != null ? fullName : "bạn", fullLink, fullLink);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Verification email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Không thể gửi email xác minh. Vui lòng thử lại sau.");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
