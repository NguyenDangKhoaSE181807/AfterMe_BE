package com.example.reminder.service.impl;

import com.example.reminder.domain.enums.VerificationCodePurpose;
import com.example.reminder.domain.enums.UserStatus;
import com.example.reminder.entity.EmailVerificationCode;
import com.example.reminder.entity.User;
import com.example.reminder.exception.BadRequestException;
import com.example.reminder.repository.EmailVerificationCodeRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.EmailService;
import com.example.reminder.service.EmailVerificationService;
import java.time.LocalDateTime;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final EmailVerificationCodeRepository emailVerificationCodeRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.mail.verification-code-expiry-minutes:15}")
    private Integer verificationCodeExpiryMinutes;

    @Override
    @Transactional
    public void generateAndSendVerificationCode(User user) {
        generateAndSendVerificationCode(user, VerificationCodePurpose.SIGN_UP);
    }

    @Override
    @Transactional
    public void generateAndSendVerificationCode(User user, VerificationCodePurpose purpose) {
        // Generate 8-digit random code
        String code = generateVerificationCode();

        // Create verification code record
        EmailVerificationCode verificationCode = EmailVerificationCode.builder()
                .user(user)
                .code(code)
                .purpose(purpose)
                .isUsed(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(verificationCodeExpiryMinutes))
                .build();

        emailVerificationCodeRepository.save(verificationCode);

        if (purpose == VerificationCodePurpose.PASSWORD_CHANGE) {
            emailService.sendPasswordChangeCode(user.getEmail(), code);
        } else {
            emailService.sendVerificationCode(user.getEmail(), code);
        }

        log.info("Verification code generated and sent for user: {}, purpose: {}", user.getEmail(), purpose);
    }

    @Override
    @Transactional
    public void verifyCode(Long userId, String code) {
        verifyCode(userId, code, VerificationCodePurpose.SIGN_UP);
    }

    @Override
    @Transactional
    public void verifyCode(Long userId, String code, VerificationCodePurpose purpose) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Find valid verification code
        EmailVerificationCode verificationCode = emailVerificationCodeRepository
                .findByUserIdAndCodeAndPurposeAndIsUsedFalseAndExpiresAtAfter(userId, code, purpose, LocalDateTime.now())
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification code"));

        // Mark code as used
        verificationCode.setIsUsed(true);
        verificationCode.setVerifiedAt(LocalDateTime.now());
        emailVerificationCodeRepository.save(verificationCode);

        if (purpose == VerificationCodePurpose.SIGN_UP) {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
            emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());
        }

        log.info("Verification code validated for user: {}, purpose: {}", user.getEmail(), purpose);
    }

    @Override
    @Transactional
    public void resendVerificationCode(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Check if user is already verified
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BadRequestException("User is already verified");
        }

        // Generate and send new code
        generateAndSendVerificationCode(user, VerificationCodePurpose.SIGN_UP);
    }

    /**
     * Generate a random 8-digit verification code
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 10000000 + random.nextInt(90000000);
        return String.valueOf(code);
    }
}
