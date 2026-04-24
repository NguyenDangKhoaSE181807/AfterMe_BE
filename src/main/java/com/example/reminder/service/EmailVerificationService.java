package com.example.reminder.service;

import com.example.reminder.entity.User;

public interface EmailVerificationService {

    void generateAndSendVerificationCode(User user);

    void verifyCode(Long userId, String code);

    void resendVerificationCode(Long userId);
}
