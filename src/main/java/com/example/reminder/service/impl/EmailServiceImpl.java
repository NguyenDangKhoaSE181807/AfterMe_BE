package com.example.reminder.service.impl;

import com.example.reminder.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@afterme.com}")
    private String fromEmail;

    @Value("${app.mail.verification-code-expiry-minutes:15}")
    private Integer verificationCodeExpiryMinutes;

    @Override
    public void sendVerificationCode(String recipientEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(recipientEmail);
            message.setSubject("AfterMe - Email Verification Code");
            message.setText(buildVerificationEmailContent(code));

            mailSender.send(message);
            log.info("Verification email sent to: {}", recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", recipientEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    @Override
    public void sendWelcomeEmail(String recipientEmail, String fullName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(recipientEmail);
            message.setSubject("Welcome to AfterMe!");
            message.setText(buildWelcomeEmailContent(fullName));

            mailSender.send(message);
            log.info("Welcome email sent to: {}", recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", recipientEmail, e);
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }

    private String buildVerificationEmailContent(String code) {
        return String.format("""
                Hello,

                Your AfterMe email verification code is: %s

                This code will expire in %d minutes.

                If you didn't request this code, please ignore this email.

                Best regards,
                The AfterMe Team
                """, code, verificationCodeExpiryMinutes);
    }

    private String buildWelcomeEmailContent(String fullName) {
        return String.format("""
                Hello %s,

                Welcome to AfterMe! Your account has been successfully created and verified.

                You can now log in and start using all features of our service.

                If you have any questions, feel free to contact our support team.

                Best regards,
                The AfterMe Team
                """, fullName);
    }
}
