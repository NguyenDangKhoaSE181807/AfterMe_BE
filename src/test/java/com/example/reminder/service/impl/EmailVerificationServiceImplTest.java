package com.example.reminder.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.reminder.domain.enums.VerificationCodePurpose;
import com.example.reminder.domain.enums.UserStatus;
import com.example.reminder.entity.EmailVerificationCode;
import com.example.reminder.entity.User;
import com.example.reminder.repository.EmailVerificationCodeRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.DailyReminderService;
import com.example.reminder.service.EmailService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EmailVerificationServiceImplTest {

    private final EmailVerificationCodeRepository emailVerificationCodeRepository = mock(EmailVerificationCodeRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final EmailService emailService = mock(EmailService.class);
    private final DailyReminderService dailyReminderService = mock(DailyReminderService.class);

    private final EmailVerificationServiceImpl service = new EmailVerificationServiceImpl(
            emailVerificationCodeRepository,
            userRepository,
            emailService,
            dailyReminderService
    );

    @Test
    void verifyCode_forSignUp_activatesUserAndCreatesDailyReminder() {
        ReflectionTestUtils.setField(service, "verificationCodeExpiryMinutes", 15);

        User user = new User();
        user.setId(42L);
        user.setEmail("new@example.com");
        user.setFullName("New User");
        user.setStatus(UserStatus.PENDING);

        EmailVerificationCode verificationCode = EmailVerificationCode.builder()
                .user(user)
                .code("12345678")
                .purpose(VerificationCodePurpose.SIGN_UP)
                .isUsed(false)
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .expiresAt(LocalDateTime.now().plusMinutes(14))
                .build();

        when(userRepository.findByIdAndDeletedAtIsNull(42L)).thenReturn(Optional.of(user));
        when(emailVerificationCodeRepository.findByUserIdAndCodeAndPurposeAndIsUsedFalseAndExpiresAtAfter(
                eq(42L),
                eq("12345678"),
                eq(VerificationCodePurpose.SIGN_UP),
                any(LocalDateTime.class)
        )).thenReturn(Optional.of(verificationCode));

        service.verifyCode(42L, "12345678");

        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verify(emailVerificationCodeRepository).save(verificationCode);
        verify(userRepository).save(user);
        verify(emailService).sendWelcomeEmail("new@example.com", "New User");
        verify(dailyReminderService).createDailyCheckInReminder(42L);
    }
}
