package com.example.reminder.service.impl;

import com.example.reminder.domain.enums.TonePreference;
import com.example.reminder.domain.enums.UserRole;
import com.example.reminder.domain.enums.SessionStatus;
import com.example.reminder.domain.enums.UserStatus;
import com.example.reminder.domain.enums.VerificationCodePurpose;
import com.example.reminder.dto.auth.AuthResponseDto;
import com.example.reminder.dto.auth.UserSessionResponseDto;
import com.example.reminder.entity.RefreshToken;
import com.example.reminder.entity.User;
import com.example.reminder.entity.UserSession;
import com.example.reminder.exception.BadRequestException;
import com.example.reminder.repository.RefreshTokenRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.repository.UserSessionRepository;
import com.example.reminder.service.AuthService;
import com.example.reminder.service.EmailVerificationService;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int REFRESH_TOKEN_BYTE_SIZE = 48;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final EmailVerificationService emailVerificationService;

    @Value("${app.security.jwt.refresh-token-ttl-seconds:1209600}")
    private long refreshTokenTtlSeconds;

    @Override
    @Transactional
    public AuthResponseDto signUp(String email, String rawPassword, String fullName, TonePreference tonePreference) {
        if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
            throw new BadRequestException("Email already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setTonePreference(tonePreference == null ? TonePreference.NORMAL : tonePreference);
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.CUSTOMER);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        UserSession session = createSession(savedUser, "signup-default", null, "signup-flow");
        String rawRefreshToken = issueRefreshToken(session);
        return toAuthResponse(savedUser, rawRefreshToken);
    }

    @Override
    @Transactional
    public Long registerUserForEmailVerification(String email, String rawPassword, String fullName, TonePreference tonePreference) {
        if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
            throw new BadRequestException("Email already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setTonePreference(tonePreference == null ? TonePreference.NORMAL : tonePreference);
        user.setStatus(UserStatus.PENDING);
        user.setRole(UserRole.CUSTOMER);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // Generate and send verification code
        emailVerificationService.generateAndSendVerificationCode(savedUser);

        return savedUser.getId();
    }

    @Override
    @Transactional
    public Long verifyEmailAndActivateUser(Long userId, String verificationCode) {
        emailVerificationService.verifyCode(userId, verificationCode);
        return userId;
    }

    @Override
    @Transactional
    public void resendVerificationCode(Long userId) {
        emailVerificationService.resendVerificationCode(userId);
    }

    @Override
    @Transactional
    public void sendPasswordChangeCode(String email) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        emailVerificationService.generateAndSendVerificationCode(user, VerificationCodePurpose.PASSWORD_CHANGE);
    }

    @Override
    @Transactional
    public void changePasswordWithCode(String email, String verificationCode, String newPassword) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        emailVerificationService.verifyCode(user.getId(), verificationCode, VerificationCodePurpose.PASSWORD_CHANGE);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public AuthResponseDto signIn(String email, String rawPassword, String deviceId, String ipAddress, String userAgent) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("User is not active");
        }

        boolean matched = passwordEncoder.matches(rawPassword, user.getPasswordHash())
                || rawPassword.equals(user.getPasswordHash());
        if (!matched) {
            throw new BadRequestException("Invalid email or password");
        }

        String normalizedDeviceId = normalizeDeviceId(deviceId);
        UserSession session = upsertActiveSession(user, normalizedDeviceId, ipAddress, userAgent);
        revokeActiveRefreshTokens(session);
        String rawRefreshToken = issueRefreshToken(session);
        return toAuthResponse(user, rawRefreshToken);
    }

    @Override
    @Transactional
    public AuthResponseDto refreshToken(String refreshToken, String deviceId, String ipAddress, String userAgent) {
        String tokenHash = hashToken(refreshToken);
        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(tokenHash, LocalDateTime.now())
                .orElseThrow(() -> new BadRequestException("Refresh token is invalid or expired"));

        UserSession session = storedToken.getSession();
        if (session.getStatus() != SessionStatus.ACTIVE
                || (session.getExpiresAt() != null && session.getExpiresAt().isBefore(LocalDateTime.now()))) {
            throw new BadRequestException("Session is inactive or expired");
        }

        String normalizedDeviceId = normalizeDeviceId(deviceId);
        if (!session.getDeviceId().equals(normalizedDeviceId)) {
            throw new BadRequestException("Refresh token does not belong to this device session");
        }

        User user = session.getUser();
        if (user.getDeletedAt() != null || user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("User is not active");
        }

        storedToken.setRevokedAt(LocalDateTime.now());
        session.setLastUsedAt(LocalDateTime.now());
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenTtlSeconds));

        String rotatedRefreshToken = issueRefreshToken(session);
        storedToken.setReplacedByTokenHash(hashToken(rotatedRefreshToken));

        refreshTokenRepository.save(storedToken);
        userSessionRepository.save(session);
        return toAuthResponse(user, rotatedRefreshToken);
    }

    @Override
    @Transactional
    public void logout(String refreshToken, String deviceId) {
        String tokenHash = hashToken(refreshToken);
        refreshTokenRepository.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(tokenHash, LocalDateTime.now())
                .ifPresent(token -> {
                    UserSession session = token.getSession();
                    if (!session.getDeviceId().equals(normalizeDeviceId(deviceId))) {
                        throw new BadRequestException("Cannot logout another device session");
                    }
                    token.setRevokedAt(LocalDateTime.now());
                    session.setStatus(SessionStatus.REVOKED);
                    session.setLastUsedAt(LocalDateTime.now());
                    session.setExpiresAt(LocalDateTime.now());
                    refreshTokenRepository.save(token);
                    userSessionRepository.save(session);
                });
    }

    private String issueRefreshToken(UserSession session) {
        byte[] randomBytes = new byte[REFRESH_TOKEN_BYTE_SIZE];
        java.security.SecureRandom secureRandom = new java.security.SecureRandom();
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setSession(session);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenTtlSeconds));
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    private AuthResponseDto toAuthResponse(User user, String refreshToken) {
        return new AuthResponseDto(
                "Bearer",
                jwtTokenService.generateAccessToken(user),
                jwtTokenService.getAccessTokenTtlSeconds(),
                refreshToken,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private String normalizeDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new BadRequestException("Device id is required");
        }
        String normalized = deviceId.trim();
        if (normalized.length() > 64) {
            throw new BadRequestException("Device id is too long");
        }
        return normalized;
    }

    private UserSession upsertActiveSession(User user, String deviceId, String ipAddress, String userAgent) {
        return userSessionRepository
                .findFirstByUserAndDeviceIdAndStatusAndExpiresAtAfter(
                        user,
                        deviceId,
                        SessionStatus.ACTIVE,
                        LocalDateTime.now()
                )
                .map(existing -> {
                    existing.setIpAddress(ipAddress);
                    existing.setUserAgent(userAgent);
                    existing.setLastUsedAt(LocalDateTime.now());
                    existing.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenTtlSeconds));
                    return userSessionRepository.save(existing);
                })
                .orElseGet(() -> createSession(user, deviceId, ipAddress, userAgent));
    }

    private UserSession createSession(User user, String deviceId, String ipAddress, String userAgent) {
        UserSession session = new UserSession();
        session.setUser(user);
        session.setDeviceId(deviceId);
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setCreatedAt(LocalDateTime.now());
        session.setLastUsedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenTtlSeconds));
        session.setStatus(SessionStatus.ACTIVE);
        return userSessionRepository.save(session);
    }

    private void revokeActiveRefreshTokens(UserSession session) {
        refreshTokenRepository.findBySessionIdAndRevokedAtIsNullAndExpiresAtAfter(session.getId(), LocalDateTime.now())
                .forEach(token -> {
                    token.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(token);
                });
    }

    @Override
    public List<UserSessionResponseDto> listActiveSessionsByUserId(Long userId, String currentDeviceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        List<UserSession> activeSessions = userSessionRepository
                .findByUserAndStatusAndExpiresAtAfterOrderByLastUsedAtDesc(
                        user,
                        SessionStatus.ACTIVE,
                        LocalDateTime.now()
                );

        return activeSessions.stream()
                .map(session -> UserSessionResponseDto.from(
                        session,
                        session.getDeviceId().equals(currentDeviceId)
                ))
                .collect(Collectors.toList());
    }
}
