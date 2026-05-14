package com.example.reminder.service;

import com.example.reminder.dto.auth.AuthResponseDto;
import com.example.reminder.dto.auth.UserSessionResponseDto;
import com.example.reminder.domain.enums.TonePreference;
import java.util.List;

public interface AuthService {

    AuthResponseDto signUp(String email, String rawPassword, String fullName, TonePreference tonePreference);

    AuthResponseDto signIn(String email, String rawPassword, String deviceId, String ipAddress, String userAgent);

    AuthResponseDto refreshToken(String refreshToken, String deviceId, String ipAddress, String userAgent);

    void logout(String refreshToken, String deviceId);

    Long registerUserForEmailVerification(String email, String rawPassword, String fullName, TonePreference tonePreference);

    Long verifyEmailAndActivateUser(Long userId, String verificationCode);

    void resendVerificationCode(Long userId);

    void sendPasswordChangeCode(String email);

    void changePasswordWithCode(String email, String verificationCode, String newPassword);

    List<UserSessionResponseDto> listActiveSessionsByUserId(Long userId, String currentDeviceId);
}
