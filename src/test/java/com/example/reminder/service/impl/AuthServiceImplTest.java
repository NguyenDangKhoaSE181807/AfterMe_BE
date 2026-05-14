package com.example.reminder.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.reminder.domain.enums.SessionStatus;
import com.example.reminder.domain.enums.UserRole;
import com.example.reminder.domain.enums.UserStatus;
import com.example.reminder.entity.RefreshToken;
import com.example.reminder.entity.User;
import com.example.reminder.entity.UserSession;
import com.example.reminder.exception.BadRequestException;
import com.example.reminder.repository.RefreshTokenRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.repository.UserSessionRepository;
import com.example.reminder.service.EmailVerificationService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class AuthServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final UserSessionRepository userSessionRepository = mock(UserSessionRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtTokenService jwtTokenService = mock(JwtTokenService.class);
    private final EmailVerificationService emailVerificationService = mock(EmailVerificationService.class);

    private final AuthServiceImpl service = new AuthServiceImpl(
            userRepository,
            refreshTokenRepository,
            userSessionRepository,
            passwordEncoder,
            jwtTokenService,
            emailVerificationService
    );

    @Test
    void refreshToken_rejectsWhenDeviceMismatch() {
        ReflectionTestUtils.setField(service, "refreshTokenTtlSeconds", 1209600L);

        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.CUSTOMER);

        UserSession session = new UserSession();
        session.setId(10L);
        session.setUser(user);
        session.setDeviceId("device-a");
        session.setStatus(SessionStatus.ACTIVE);
        session.setExpiresAt(LocalDateTime.now().plusDays(1));

        RefreshToken token = new RefreshToken();
        token.setSession(session);
        token.setTokenHash(hashToken("raw-refresh-token"));
        token.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
            eq(token.getTokenHash()),
            any(LocalDateTime.class)
        )).thenReturn(Optional.of(token));

        assertThrows(BadRequestException.class,
                () -> service.refreshToken("raw-refresh-token", "device-b", "127.0.0.1", "JUnit-Agent"));
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
