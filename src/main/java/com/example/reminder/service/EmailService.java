package com.example.reminder.service;

public interface EmailService {

    void sendVerificationCode(String recipientEmail, String code);

    void sendWelcomeEmail(String recipientEmail, String fullName);
}
