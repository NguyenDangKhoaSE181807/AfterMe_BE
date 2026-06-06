package com.example.reminder.service.impl;

import com.example.reminder.config.NotificationProperties;
import com.example.reminder.domain.enums.ActivityLogType;
import com.example.reminder.dto.activity.ActivityLogResponseDto;
import com.example.reminder.entity.ActivityLog;
import com.example.reminder.entity.User;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.repository.ActivityLogRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.ActivityLogService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationProperties notificationProperties;

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityLogResponseDto> findCurrentUserLogs(Long userId, Pageable pageable) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (isFreePlan(user)) {
            return activityLogRepository
                    .findByUserIdAndDeletedAtIsNullAndCreatedAtGreaterThanEqual(
                            userId,
                            LocalDateTime.now().minusDays(3),
                            pageable
                    )
                    .map(this::toDto);
        }
        return activityLogRepository.findByUserIdAndDeletedAtIsNull(userId, pageable).map(this::toDto);
    }

    @Override
    @Transactional
    public ActivityLogResponseDto record(
            Long userId,
            ActivityLogType type,
            String title,
            String message,
            Long reminderId,
            Long scheduleId,
            Long instanceId,
            String metadata
    ) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        ActivityLog activityLog = new ActivityLog();
        activityLog.setUser(user);
        activityLog.setType(type);
        activityLog.setTitle(title);
        activityLog.setMessage(message);
        activityLog.setReminderId(reminderId);
        activityLog.setScheduleId(scheduleId);
        activityLog.setInstanceId(instanceId);
        activityLog.setMetadata(metadata);
        activityLog.setCreatedAt(LocalDateTime.now());

        ActivityLogResponseDto dto = toDto(activityLogRepository.save(activityLog));
        messagingTemplate.convertAndSend(activityDestination(userId), dto);
        return dto;
    }

    private String activityDestination(Long userId) {
        return notificationProperties.websocket().topicPrefix() + "/activities/" + userId;
    }

    private boolean isFreePlan(User user) {
        String planName = user.getCurrentPlan() == null ? "FREE" : user.getCurrentPlan().getName();
        return planName == null
                || planName.equalsIgnoreCase("FREE")
                || planName.equalsIgnoreCase("FREEMIUM");
    }

    private ActivityLogResponseDto toDto(ActivityLog activityLog) {
        return new ActivityLogResponseDto(
                activityLog.getId(),
                activityLog.getUser().getId(),
                activityLog.getType(),
                activityLog.getTitle(),
                activityLog.getMessage(),
                activityLog.getReminderId(),
                activityLog.getScheduleId(),
                activityLog.getInstanceId(),
                activityLog.getMetadata(),
                activityLog.getCreatedAt()
        );
    }
}
