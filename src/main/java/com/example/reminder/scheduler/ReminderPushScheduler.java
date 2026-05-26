package com.example.reminder.scheduler;

import com.example.reminder.domain.enums.ReminderSourceType;
import com.example.reminder.domain.enums.ReminderInstanceStatus;
import com.example.reminder.entity.ReminderInstance;
import com.example.reminder.repository.ReminderInstanceRepository;
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
public class ReminderPushScheduler {

    private final ReminderInstanceRepository reminderInstanceRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void process() {
        LocalDateTime now = LocalDateTime.now();
        pushDueInstances(now, ReminderSourceType.SYSTEM, true);
        pushDueInstances(now, ReminderSourceType.USER, false);
    }

    private void pushDueInstances(LocalDateTime now, ReminderSourceType sourceType, boolean requiresResponse) {
        List<ReminderInstance> dueInstances = reminderInstanceRepository.findDueForInitialPush(now, sourceType);
        log.debug("ReminderPushScheduler run for {}: {} due instances", sourceType, dueInstances.size());

        for (ReminderInstance instance : dueInstances) {
            try {
                notificationService.send(new com.example.reminder.dto.notification.SendNotificationRequest(
                        instance.getReminder().getUser().getId(),
                        instance.getReminder().getTitle(),
                        instance.getReminder().getDescription(),
                        instance.getReminder().getId(),
                        instance.getSchedule() == null ? null : instance.getSchedule().getId(),
                        instance.getId(),
                        sourceType,
                        requiresResponse
                ));

                instance.setLastNotificationAt(now);
                if (requiresResponse && instance.getStatus() == ReminderInstanceStatus.PENDING) {
                    instance.setNextRemindAt(now.plusHours(1));
                }
                reminderInstanceRepository.save(instance);
            } catch (Exception ex) {
                log.error("Failed to send initial notification for instance {}: {}", instance.getId(), ex.getMessage(), ex);
            }
        }
    }
}