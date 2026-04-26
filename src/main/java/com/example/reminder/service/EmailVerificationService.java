package com.example.reminder.service;

import com.example.reminder.domain.enums.VerificationCodePurpose;
import com.example.reminder.entity.User;

public interface EmailVerificationService {

    void generateAndSendVerificationCode(User user);

    void generateAndSendVerificationCode(User user, VerificationCodePurpose purpose);

    void verifyCode(Long userId, String code);

    void verifyCode(Long userId, String code, VerificationCodePurpose purpose);

    void resendVerificationCode(Long userId);
}
