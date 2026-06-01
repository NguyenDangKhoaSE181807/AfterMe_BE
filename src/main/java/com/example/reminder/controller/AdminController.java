package com.example.reminder.controller;

import com.example.reminder.dto.common.BaseResponse;
import com.example.reminder.entity.ReminderInstance;
import com.example.reminder.entity.User;
import com.example.reminder.entity.UserSafetyState;
import com.example.reminder.domain.enums.RiskLevel;
import com.example.reminder.domain.enums.UserRole;
import com.example.reminder.exception.ForbiddenException;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.repository.ReminderInstanceRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.repository.UserSafetyStateRepository;
import com.example.reminder.service.SafetyAlertService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final UserSafetyStateRepository userSafetyStateRepository;
    private final ReminderInstanceRepository reminderInstanceRepository;
    private final SafetyAlertService safetyAlertService;

    @PostMapping("/users/{userId}/safety/increment-missed")
    @Transactional
    public ResponseEntity<BaseResponse<UserSafetyState>> incrementMissed(
            @PathVariable Long userId,
            Authentication authentication,
            HttpServletRequest request
    ) {
        User requester = getCurrentUser(authentication);
        if (requester.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Admin role required");
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        UserSafetyState safetyState = userSafetyStateRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserSafetyState created = new UserSafetyState();
                    created.setUser(user);
                    created.setCreatedAt(LocalDateTime.now());
                    return created;
                });

        RiskLevel previousRisk = safetyState.getRiskLevel();

        int consecutiveMissed = safetyState.getConsecutiveMissedCount() == null ? 0 : safetyState.getConsecutiveMissedCount();
        consecutiveMissed += 1;
        safetyState.setConsecutiveMissedCount(consecutiveMissed);
        safetyState.setLastMissedAt(LocalDateTime.now());

        RiskLevel newRisk;
        if (consecutiveMissed <= 0) newRisk = RiskLevel.LOW;
        else if (consecutiveMissed == 1) newRisk = RiskLevel.MEDIUM;
        else if (consecutiveMissed == 2) newRisk = RiskLevel.HIGH;
        else newRisk = RiskLevel.CRITICAL;

        safetyState.setRiskLevel(newRisk);
        safetyState.setUpdatedAt(LocalDateTime.now());
        userSafetyStateRepository.save(safetyState);

        boolean alertTriggered = false;

        // trigger alert if transitioned to CRITICAL
        if (previousRisk != RiskLevel.CRITICAL && newRisk == RiskLevel.CRITICAL) {
            Optional<ReminderInstance> latestInstance = reminderInstanceRepository.findTopByReminderUserIdAndDeletedAtIsNullOrderByScheduledTimeDesc(userId);
            if (latestInstance.isPresent()) {
                safetyAlertService.triggerSafetyAlert(user, latestInstance.get());
                alertTriggered = true;
            }
        }

        BaseResponse<UserSafetyState> body = BaseResponse.<UserSafetyState>builder()
                .success(true)
                .code("ADMIN_INCREMENT_MISSED_SUCCESS")
                .message(alertTriggered
                        ? "Incremented missed count and triggered safety alert"
                        : "Incremented missed count")
                .data(safetyState)
                .errors(null)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .requestId(request.getHeader("X-Request-Id"))
                .build();

        return ResponseEntity.ok(body);
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenException("User must be authenticated");
        }

        String email = authentication.getName();
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}
