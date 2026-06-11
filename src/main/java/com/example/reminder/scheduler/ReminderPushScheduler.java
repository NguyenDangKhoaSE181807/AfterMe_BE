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
        log.info("ReminderPushScheduler tick: now={}", now);
        pushDueInstances(now, ReminderSourceType.SYSTEM);
        pushDueInstances(now, ReminderSourceType.USER);
    }

    private void pushDueInstances(LocalDateTime now, ReminderSourceType sourceType) {
        List<ReminderInstance> dueInstances = reminderInstanceRepository.findDueForInitialPush(now, sourceType);
        log.info("ReminderPushScheduler due instances: sourceType={}, count={}", sourceType, dueInstances.size());

        for (ReminderInstance instance : dueInstances) {
            boolean requiresResponse = requiresResponse(instance);
            try {
                log.info("ReminderPushScheduler sending instance: instanceId={}, reminderId={}, userId={}, sourceType={}, requiresResponse={}",
                        instance.getId(),
                        instance.getReminder().getId(),
                        instance.getReminder().getUser().getId(),
                        sourceType,
                        requiresResponse);

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
                    instance.setNextRemindAt(resolveInitialNextRemindAt(instance));
                } else if (!requiresResponse && instance.getStatus() == ReminderInstanceStatus.PENDING) {
                    instance.setStatus(ReminderInstanceStatus.DONE);
                    instance.setResolvedAt(now);
                    instance.setNextRemindAt(null);
                }
                reminderInstanceRepository.save(instance);
            } catch (Exception ex) {
                log.error("Failed to send initial notification for instance {}: {}", instance.getId(), ex.getMessage(), ex);
            }
        }
    }

    private boolean requiresResponse(ReminderInstance instance) {
        return instance.getReminder().getSourceType() == ReminderSourceType.SYSTEM
                || Boolean.TRUE.equals(instance.getReminder().getSafetyEnabled());
    }

    private LocalDateTime resolveInitialNextRemindAt(ReminderInstance instance) {
        if (instance.getReminder().getSourceType() == ReminderSourceType.USER
                && Boolean.TRUE.equals(instance.getReminder().getSafetyEnabled())) {
            return instance.getScheduledTime().plusMinutes(15);
        }
        return instance.getScheduledTime().plusMinutes(5);
    }
}
