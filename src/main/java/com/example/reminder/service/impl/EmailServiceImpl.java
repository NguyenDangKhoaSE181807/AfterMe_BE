package com.example.reminder.service.impl;

import com.example.reminder.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
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
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(recipientEmail);
            helper.setSubject("AfterMe - Email Verification Code");
            helper.setText(buildVerificationEmailHtml(code), true);

            mailSender.send(message);
            log.info("Verification email sent to: {}", recipientEmail);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to: {}", recipientEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    @Override
    public void sendPasswordChangeCode(String recipientEmail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(recipientEmail);
            helper.setSubject("AfterMe - Password Change Verification Code");
            helper.setText(buildPasswordChangeEmailHtml(code), true);

            mailSender.send(message);
            log.info("Password change verification email sent to: {}", recipientEmail);
        } catch (MessagingException e) {
            log.error("Failed to send password change verification email to: {}", recipientEmail, e);
            throw new RuntimeException("Failed to send password change verification email", e);
        }
    }

    @Override
    public void sendWelcomeEmail(String recipientEmail, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(recipientEmail);
            helper.setSubject("Welcome to AfterMe!");
            helper.setText(buildWelcomeEmailHtml(fullName), true);

            mailSender.send(message);
            log.info("Welcome email sent to: {}", recipientEmail);
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to: {}", recipientEmail, e);
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }

    private String buildVerificationEmailHtml(String code) {
        return buildCodeEmailHtml(
                "Email Verification Code",
                "Use this code to verify your new account.",
                code,
                "This code will expire in %d minutes.",
                "If you didn't request this code, please ignore this email."
        );
    }

    private String buildWelcomeEmailHtml(String fullName) {
        return String.format("""
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                </head>
                <body style="margin:0;padding:0;background:#e8fbf8;font-family:Arial,Helvetica,sans-serif;color:#134e4a;">
                    <div style="max-width:640px;margin:0 auto;padding:32px 16px;">
                        <div style="background:#ffffff;border:1px solid #bfeee6;border-radius:18px;overflow:hidden;box-shadow:0 10px 30px rgba(16,185,129,0.12);">
                            <div style="background:linear-gradient(135deg,#0f766e,#14b8a6);padding:28px 32px;color:#ffffff;">
                                <div style="font-size:14px;letter-spacing:1.8px;text-transform:uppercase;opacity:0.9;">AfterMe</div>
                                <h1 style="margin:8px 0 0;font-size:28px;line-height:1.2;">Welcome to your account</h1>
                            </div>
                            <div style="padding:32px;">
                                <p style="margin:0 0 16px;font-size:16px;line-height:1.7;">Hello %s,</p>
                                <p style="margin:0 0 16px;font-size:16px;line-height:1.7;">Your account has been successfully created and verified. You can now log in and start using all features of AfterMe.</p>
                                <div style="margin:24px 0 0;padding:16px 18px;border-left:4px solid #14b8a6;background:#f0fdfa;border-radius:12px;">
                                    <p style="margin:0;font-size:14px;line-height:1.6;color:#0f766e;">If you have any questions, feel free to contact our support team.</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """, fullName);
    }

    private String buildPasswordChangeEmailHtml(String code) {
        return buildCodeEmailHtml(
                "Password Change Verification Code",
                "Use this code to confirm your password change.",
                code,
                "This code will expire in %d minutes.",
                "If you didn't request this action, please secure your account immediately."
        );
    }

    private String buildCodeEmailHtml(
            String title,
            String intro,
            String code,
            String expiryLineTemplate,
            String footerNote
    ) {
        return String.format("""
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                </head>
                <body style="margin:0;padding:0;background:#e8fbf8;font-family:Arial,Helvetica,sans-serif;color:#134e4a;">
                    <div style="max-width:640px;margin:0 auto;padding:32px 16px;">
                        <div style="background:#ffffff;border:1px solid #bfeee6;border-radius:18px;overflow:hidden;box-shadow:0 10px 30px rgba(16,185,129,0.12);">
                            <div style="background:linear-gradient(135deg,#0f766e,#14b8a6);padding:28px 32px;color:#ffffff;">
                                <div style="font-size:14px;letter-spacing:1.8px;text-transform:uppercase;opacity:0.9;">AfterMe</div>
                                <h1 style="margin:8px 0 0;font-size:28px;line-height:1.2;">%s</h1>
                            </div>
                            <div style="padding:32px;">
                                <p style="margin:0 0 16px;font-size:16px;line-height:1.7;">Hello,</p>
                                <p style="margin:0 0 20px;font-size:16px;line-height:1.7;">%s</p>
                                <div style="margin:24px 0;padding:20px;border:1px dashed #14b8a6;border-radius:16px;background:#f0fdfa;text-align:center;">
                                    <div style="font-size:13px;letter-spacing:1.6px;text-transform:uppercase;color:#0f766e;margin-bottom:10px;">Verification Code</div>
                                    <div style="font-size:34px;font-weight:700;letter-spacing:6px;color:#0f766e;">%s</div>
                                </div>
                                <p style="margin:0 0 10px;font-size:14px;line-height:1.6;color:#0f766e;">%s</p>
                                <p style="margin:0;font-size:14px;line-height:1.6;color:#0f766e;">%s</p>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """,
                title,
                intro,
                code,
                String.format(expiryLineTemplate, verificationCodeExpiryMinutes),
                footerNote
        );
    }
}
