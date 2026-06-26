package com.example.reminder.service.impl;

import com.example.reminder.domain.enums.UserActivitySignalType;
import com.example.reminder.dto.activity.PassiveActivitySettingsResponseDto;
import com.example.reminder.dto.activity.UserActivitySignalRequest;
import com.example.reminder.dto.activity.UserActivityStateResponseDto;
import com.example.reminder.entity.User;
import com.example.reminder.entity.UserActivityState;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.repository.UserActivityStateRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.PassiveActivityService;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PassiveActivityServiceImpl implements PassiveActivityService {

    private final UserActivityStateRepository userActivityStateRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserActivityStateResponseDto recordSignal(Long userId, UserActivitySignalRequest request) {
        User user = getUser(userId);
        LocalDateTime now = LocalDateTime.now();
        UserActivityState state = userActivityStateRepository.findByUserIdAndDeviceId(userId, request.deviceId())
                .orElseGet(() -> {
                    UserActivityState created = new UserActivityState();
                    created.setUser(user);
                    created.setDeviceId(request.deviceId());
                    created.setCreatedAt(now);
                    return created;
                });

        LocalDateTime occurredAt = request.occurredAt().withNano(0);
        switch (request.signalType()) {
            case APP_FOREGROUND -> state.setLastAppForegroundAt(max(state.getLastAppForegroundAt(), occurredAt));
            case APP_INTERACTION -> state.setLastAppInteractionAt(max(state.getLastAppInteractionAt(), occurredAt));
            case PUSH_TAPPED -> state.setLastPushTappedAt(max(state.getLastPushTappedAt(), occurredAt));
            case DEVICE_UNLOCKED -> state.setLastDeviceUnlockedAt(max(state.getLastDeviceUnlockedAt(), occurredAt));
            case DEVICE_INTERACTIVE -> state.setLastDeviceInteractiveAt(max(state.getLastDeviceInteractiveAt(), occurredAt));
            case MOTION_DETECTED, ACTIVITY_RECOGNITION -> state.setLastMotionAt(max(state.getLastMotionAt(), occurredAt));
        }
        state.setLastActivityType(request.signalType());
        state.setUpdatedAt(now);
        return toDto(userActivityStateRepository.save(state));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserActivityStateResponseDto> getMyActivityStates(Long userId) {
        return userActivityStateRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(UserActivityState::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public PassiveActivitySettingsResponseDto updateSettings(Long userId, boolean enabled) {
        User user = getUser(userId);
        user.setPassiveActivityAssistEnabled(enabled);
        userRepository.save(user);
        return new PassiveActivitySettingsResponseDto(user.getPassiveActivityAssistEnabled());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RecentActivityEvidence> findRecentStrongActivity(Long userId, LocalDateTime after, LocalDateTime before, long lookbackMinutes) {
        if (!isPassiveActivityEnabled(userId)) {
            return Optional.empty();
        }
        LocalDateTime cutoff = before.minusMinutes(lookbackMinutes);
        LocalDateTime lowerBound = after == null ? cutoff : (after.isAfter(cutoff) ? after : cutoff);
        return userActivityStateRepository.findByUserId(userId).stream()
                .flatMap(state -> List.of(
                        evidence(UserActivitySignalType.APP_INTERACTION, state.getLastAppInteractionAt(), "Ban vua tuong tac voi ung dung AfterMe gan day."),
                        evidence(UserActivitySignalType.APP_FOREGROUND, state.getLastAppForegroundAt(), "Ban vua mo ung dung AfterMe gan day."),
                        evidence(UserActivitySignalType.PUSH_TAPPED, state.getLastPushTappedAt(), "Ban vua bam thong bao AfterMe gan day.")
                ).stream())
                .flatMap(Optional::stream)
                .filter(evidence -> !evidence.occurredAt().isBefore(lowerBound) && !evidence.occurredAt().isAfter(before))
                .max(Comparator.comparing(RecentActivityEvidence::occurredAt));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPassiveActivityEnabled(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .map(User::getPassiveActivityAssistEnabled)
                .map(Boolean.TRUE::equals)
                .orElse(false);
    }

    private User getUser(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private Optional<RecentActivityEvidence> evidence(UserActivitySignalType type, LocalDateTime occurredAt, String reason) {
        return occurredAt == null ? Optional.empty() : Optional.of(new RecentActivityEvidence(type, occurredAt, reason));
    }

    private LocalDateTime max(LocalDateTime current, LocalDateTime candidate) {
        if (current == null || candidate.isAfter(current)) {
            return candidate;
        }
        return current;
    }

    private UserActivityStateResponseDto toDto(UserActivityState state) {
        return new UserActivityStateResponseDto(
                state.getDeviceId(),
                state.getLastAppForegroundAt(),
                state.getLastAppInteractionAt(),
                state.getLastPushTappedAt(),
                state.getLastDeviceUnlockedAt(),
                state.getLastDeviceInteractiveAt(),
                state.getLastMotionAt(),
                state.getLastActivityType(),
                state.getUpdatedAt()
        );
    }
}
