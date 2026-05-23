package com.example.reminder.service.impl;

import com.example.reminder.dto.securitypin.UserPinStatusResponse;
import com.example.reminder.entity.User;
import com.example.reminder.exception.BadRequestException;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.UserPinService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPinServiceImpl implements UserPinService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.pin.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.security.pin.lock-minutes:15}")
    private int lockMinutes;

    @Override
    @Transactional
    public void setPin(Long userId, String pin) {
        User user = loadActiveUser(userId);
        user.setPinHash(passwordEncoder.encode(pin));
        user.setPinFailedAttempts(0);
        user.setPinLockedUntil(null);
        user.setPinUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void verifyPin(Long userId, String pin) {
        User user = loadActiveUser(userId);
        LocalDateTime now = LocalDateTime.now();

        if (user.getPinHash() == null || user.getPinHash().isBlank()) {
            throw new BadRequestException("PIN is not configured");
        }

        if (user.getPinLockedUntil() != null && user.getPinLockedUntil().isAfter(now)) {
            throw new BadRequestException("PIN is temporarily locked");
        }

        if (passwordEncoder.matches(pin, user.getPinHash())) {
            user.setPinFailedAttempts(0);
            user.setPinLockedUntil(null);
            userRepository.save(user);
            return;
        }

        int attempts = user.getPinFailedAttempts() == null ? 0 : user.getPinFailedAttempts();
        attempts += 1;
        user.setPinFailedAttempts(attempts);

        if (attempts >= Math.max(1, maxAttempts)) {
            user.setPinLockedUntil(now.plusMinutes(Math.max(1, lockMinutes)));
        }

        userRepository.save(user);
        throw new BadRequestException("Invalid PIN");
    }

    @Override
    @Transactional(readOnly = true)
    public UserPinStatusResponse getStatus(Long userId) {
        User user = loadActiveUser(userId);
        boolean configured = user.getPinHash() != null && !user.getPinHash().isBlank();
        int attempts = user.getPinFailedAttempts() == null ? 0 : user.getPinFailedAttempts();
        int remaining = Math.max(0, Math.max(1, maxAttempts) - attempts);
        return new UserPinStatusResponse(configured, remaining, user.getPinLockedUntil());
    }

    private User loadActiveUser(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
