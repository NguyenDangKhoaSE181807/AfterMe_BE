package com.example.reminder.service.impl;

import com.example.reminder.domain.enums.TonePreference;
import com.example.reminder.domain.enums.UserRole;
import com.example.reminder.domain.enums.UserStatus;
import com.example.reminder.dto.auth.AuthResponseDto;
import com.example.reminder.entity.RefreshToken;
import com.example.reminder.entity.User;
import com.example.reminder.exception.BadRequestException;
import com.example.reminder.repository.RefreshTokenRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.AuthService;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
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
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

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
        String rawRefreshToken = issueRefreshToken(savedUser);
        return toAuthResponse(savedUser, rawRefreshToken);
    }

    @Override
    @Transactional
    public AuthResponseDto signIn(String email, String rawPassword) {
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

        String rawRefreshToken = issueRefreshToken(user);
        return toAuthResponse(user, rawRefreshToken);
    }

    @Override
    @Transactional
    public AuthResponseDto refreshToken(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(tokenHash, LocalDateTime.now())
                .orElseThrow(() -> new BadRequestException("Refresh token is invalid or expired"));

        User user = storedToken.getUser();
        if (user.getDeletedAt() != null || user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("User is not active");
        }

        storedToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(storedToken);

        String rotatedRefreshToken = issueRefreshToken(user);
        return toAuthResponse(user, rotatedRefreshToken);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        refreshTokenRepository.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(tokenHash, LocalDateTime.now())
                .ifPresent(token -> {
                    token.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(token);
                });
    }

    private String issueRefreshToken(User user) {
        byte[] randomBytes = new byte[REFRESH_TOKEN_BYTE_SIZE];
        java.security.SecureRandom secureRandom = new java.security.SecureRandom();
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
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
}
