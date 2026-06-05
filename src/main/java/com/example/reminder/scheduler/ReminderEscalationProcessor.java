package com.example.reminder.scheduler;

import com.example.reminder.entity.ReminderInstance;
import com.example.reminder.entity.EscalationLog;
import com.example.reminder.entity.UserSafetyState;
import com.example.reminder.domain.enums.RiskLevel;
import com.example.reminder.repository.ReminderInstanceRepository;
import com.example.reminder.repository.EscalationLogRepository;
import com.example.reminder.repository.UserSafetyStateRepository;
import com.example.reminder.service.SafetyAlertService;
import com.example.reminder.service.NotificationService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.safety.legacy-escalation", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class ReminderEscalationProcessor {

    private final ReminderInstanceRepository reminderInstanceRepository;
    private final EscalationLogRepository escalationLogRepository;
    private final NotificationService notificationService;
    private final UserSafetyStateRepository userSafetyStateRepository;
    private final SafetyAlertService safetyAlertService;

    // Runs every minute to evaluate pending reminders
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void process() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);

        List<ReminderInstance> due = reminderInstanceRepository.findDueForEscalation(now, oneHourAgo, com.example.reminder.domain.enums.ReminderSourceType.SYSTEM);
        log.debug("ReminderEscalationProcessor run: {} due instances", due.size());

        for (ReminderInstance instance : due) {
            try {
                // Determine whether this is a PUSH_AGAIN (missed) or escalate step
                boolean shouldPushAgain = (instance.getNextRemindAt() != null && !instance.getNextRemindAt().isAfter(now))
                        || (instance.getNextRemindAt() == null && instance.getLastNotificationAt() != null && !instance.getLastNotificationAt().isAfter(oneHourAgo));

                if (shouldPushAgain) {
                    // increment escalation level
                    int nextLevel = instance.getEscalationLevel() + 1;
                    instance.setEscalationLevel(nextLevel);
                    instance.setNextRemindAt(now.plusHours(1));
                    instance.setLastNotificationAt(now);
                    reminderInstanceRepository.save(instance);

                    EscalationLog logEntry = new EscalationLog();
                    logEntry.setReminderInstance(instance);
                    logEntry.setLevel(nextLevel);
                    logEntry.setNotificationType(com.example.reminder.domain.enums.NotificationType.SOUND);
                    logEntry.setTriggeredAt(now);
                    escalationLogRepository.save(logEntry);

                    // send notification via NotificationService
                    notificationService.send(new com.example.reminder.dto.notification.SendNotificationRequest(instance.getReminder().getUser().getId(),
                            instance.getReminder().getTitle(),
                            instance.getReminder().getDescription(),
                            instance.getReminder().getId(),
                            instance.getSchedule() == null ? null : instance.getSchedule().getId(),
                            instance.getId(),
                            instance.getReminder().getSourceType(),
                            Boolean.TRUE));
                }

                // If it's deadline and still not resolved, mark as MISSED
                LocalDateTime deadline = instance.getResponseDeadline();
                if (now.isAfter(deadline) && instance.getStatus() != com.example.reminder.domain.enums.ReminderInstanceStatus.COMPLETED
                        && instance.getStatus() != com.example.reminder.domain.enums.ReminderInstanceStatus.MISSED) {
                    instance.setStatus(com.example.reminder.domain.enums.ReminderInstanceStatus.MISSED);
                    instance.setMissedCount(instance.getMissedCount() + 1);
                    instance.setNextRemindAt(null);
                    instance.setLastNotificationAt(now);
                    reminderInstanceRepository.save(instance);
                    updateUserSafetyStateOnMissed(instance, now);

                    EscalationLog missedLog = new EscalationLog();
                    missedLog.setReminderInstance(instance);
                    missedLog.setLevel(instance.getEscalationLevel());
                    missedLog.setNotificationType(com.example.reminder.domain.enums.NotificationType.COUNTDOWN);
                    missedLog.setTriggeredAt(now);
                    escalationLogRepository.save(missedLog);
                }
            } catch (Exception ex) {
                log.error("Failed to process escalation for instance {}: {}", instance.getId(), ex.getMessage(), ex);
            }
        }
    }

    private void updateUserSafetyStateOnMissed(ReminderInstance instance, LocalDateTime now) {
        Long userId = instance.getReminder().getUser().getId();
        UserSafetyState safetyState = userSafetyStateRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserSafetyState created = new UserSafetyState();
                    created.setUser(instance.getReminder().getUser());
                    created.setCreatedAt(now);
                    return created;
                });

        // remember previous risk to detect transition to CRITICAL
        com.example.reminder.domain.enums.RiskLevel previousRisk = safetyState.getRiskLevel();

        int consecutiveMissedCount = safetyState.getConsecutiveMissedCount() == null ? 0 : safetyState.getConsecutiveMissedCount();
        consecutiveMissedCount += 1;
        safetyState.setConsecutiveMissedCount(consecutiveMissedCount);
        safetyState.setLastMissedAt(now);
        com.example.reminder.domain.enums.RiskLevel newRisk = resolveRiskLevel(consecutiveMissedCount);
        safetyState.setRiskLevel(newRisk);
        safetyState.setUpdatedAt(now);
        userSafetyStateRepository.save(safetyState);

        // Trigger safety alert when user just transitioned to CRITICAL
        try {
            if (previousRisk != com.example.reminder.domain.enums.RiskLevel.CRITICAL
                    && newRisk == com.example.reminder.domain.enums.RiskLevel.CRITICAL) {
                safetyAlertService.triggerSafetyAlert(safetyState.getUser(), instance);
            }
        } catch (Exception ex) {
            log.error("Failed to trigger safety alert for user {}: {}", userId, ex.getMessage(), ex);
        }
    }

    private RiskLevel resolveRiskLevel(int consecutiveMissedCount) {
        if (consecutiveMissedCount <= 0) {
            return RiskLevel.LOW;
        }
        if (consecutiveMissedCount == 1) {
            return RiskLevel.MEDIUM;
        }
        if (consecutiveMissedCount == 2) {
            return RiskLevel.HIGH;
        }
        return RiskLevel.CRITICAL;
    }
}
