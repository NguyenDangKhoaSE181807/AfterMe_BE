package com.example.reminder.service;

public interface EmailService {

    void sendVerificationCode(String recipientEmail, String code);

    void sendPasswordChangeCode(String recipientEmail, String code);

    void sendWelcomeEmail(String recipientEmail, String fullName);

    void sendFamilyMemberAddedEmail(String recipientEmail, String familyOwnerName, String planName);

    void sendFamilyMemberInvitationEmail(String recipientEmail, String password, String planName);

    // Sends a generic safety alert email to a trusted contact (HTML body allowed)
    void sendSafetyAlertEmail(String recipientEmail, String subject, String htmlBody);
}
