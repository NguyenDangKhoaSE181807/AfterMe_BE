package com.example.reminder.scheduler;

import com.example.reminder.entity.ReminderInstance;
import com.example.reminder.entity.EscalationLog;
import com.example.reminder.repository.ReminderInstanceRepository;
import com.example.reminder.repository.EscalationLogRepository;
import com.example.reminder.service.NotificationService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderEscalationProcessor {

    private final ReminderInstanceRepository reminderInstanceRepository;
    private final EscalationLogRepository escalationLogRepository;
    private final NotificationService notificationService;

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
                            instance.getReminder().getDescription()));
                }

                // If it's past 2am next day from scheduledTime and still not resolved, mark as MISSED
                LocalDateTime twoAmNextDay = instance.getScheduledTime().plusDays(1).withHour(2).withMinute(0).withSecond(0).withNano(0);
                if (now.isAfter(twoAmNextDay) && instance.getStatus() != com.example.reminder.domain.enums.ReminderInstanceStatus.COMPLETED
                        && instance.getStatus() != com.example.reminder.domain.enums.ReminderInstanceStatus.MISSED) {
                    instance.setStatus(com.example.reminder.domain.enums.ReminderInstanceStatus.MISSED);
                    instance.setMissedCount(instance.getMissedCount() + 1);
                    instance.setNextRemindAt(null);
                    instance.setLastNotificationAt(now);
                    reminderInstanceRepository.save(instance);

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
}
