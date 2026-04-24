package com.example.reminder.service;

import com.example.reminder.dto.auth.AuthResponseDto;
import com.example.reminder.domain.enums.TonePreference;

public interface AuthService {

    AuthResponseDto signUp(String email, String rawPassword, String fullName, TonePreference tonePreference);

    AuthResponseDto signIn(String email, String rawPassword);

    AuthResponseDto refreshToken(String refreshToken);

    void logout(String refreshToken);

    Long registerUserForEmailVerification(String email, String rawPassword, String fullName, TonePreference tonePreference);

    Long verifyEmailAndActivateUser(Long userId, String verificationCode);

    void resendVerificationCode(Long userId);

    void sendPasswordChangeCode(String email);

    void changePasswordWithCode(String email, String verificationCode, String newPassword);
}
