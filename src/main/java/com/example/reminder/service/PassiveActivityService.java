package com.example.reminder.service;

import com.example.reminder.domain.enums.UserActivitySignalType;
import com.example.reminder.dto.activity.PassiveActivitySettingsResponseDto;
import com.example.reminder.dto.activity.UserActivitySignalRequest;
import com.example.reminder.dto.activity.UserActivityStateResponseDto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PassiveActivityService {

    UserActivityStateResponseDto recordSignal(Long userId, UserActivitySignalRequest request);

    List<UserActivityStateResponseDto> getMyActivityStates(Long userId);

    PassiveActivitySettingsResponseDto updateSettings(Long userId, boolean enabled);

    Optional<RecentActivityEvidence> findRecentStrongActivity(Long userId, LocalDateTime after, LocalDateTime before, long lookbackMinutes);

    boolean isPassiveActivityEnabled(Long userId);

    record RecentActivityEvidence(
            UserActivitySignalType signalType,
            LocalDateTime occurredAt,
            String reason
    ) {
    }
}
