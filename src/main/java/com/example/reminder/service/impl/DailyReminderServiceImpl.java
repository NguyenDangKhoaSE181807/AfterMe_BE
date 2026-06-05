package com.example.reminder.service.impl;

import com.example.reminder.domain.enums.ReminderSourceType;
import com.example.reminder.domain.enums.ReminderStatus;
import com.example.reminder.domain.enums.ScheduleType;
import com.example.reminder.domain.enums.TonePreference;
import com.example.reminder.entity.Reminder;
import com.example.reminder.entity.ReminderInstance;
import com.example.reminder.entity.ReminderSchedule;
import com.example.reminder.entity.User;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.repository.ReminderInstanceRepository;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.ReminderScheduleRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.DailyReminderService;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyReminderServiceImpl implements DailyReminderService {

    private final ReminderRepository reminderRepository;
    private final ReminderScheduleRepository reminderScheduleRepository;
    private final ReminderInstanceRepository reminderInstanceRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void createDailyCheckInReminder(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (reminderRepository.findFirstByUserIdAndSourceTypeAndDeletedAtIsNullOrderByCreatedAtAsc(userId, ReminderSourceType.SYSTEM).isPresent()) {
            log.info("Daily check-in reminder already exists for user: {}", userId);
            return;
        }

        LocalDateTime nextCheckInTime = nextOccurrence(user.getDailyCheckInTime());

        Reminder reminder = new Reminder();
        reminder.setUser(user);
        reminder.setTitle("Daily Check-in");
        reminder.setDescription("Daily check-in reminder to confirm that you are safe");
        reminder.setTone(user.getTonePreference() != null ? user.getTonePreference() : TonePreference.NORMAL);
        reminder.setSafetyEnabled(true);
        reminder.setStatus(ReminderStatus.ACTIVE);
        reminder.setSourceType(ReminderSourceType.SYSTEM);
        reminder.setCreatedAt(LocalDateTime.now());

        Reminder savedReminder = reminderRepository.save(reminder);
        log.info("Created daily check-in reminder for user: {}", userId);

        ReminderSchedule schedule = new ReminderSchedule();
        schedule.setReminder(savedReminder);
        schedule.setType(ScheduleType.DAILY);
        schedule.setIntervalValue(1);
        schedule.setStartDatetime(nextCheckInTime);
        schedule.setEndDatetime(null);
        schedule.setCreatedAt(LocalDateTime.now());

        ReminderSchedule savedSchedule = reminderScheduleRepository.save(schedule);
        log.info("Created daily schedule for reminder: {}", savedReminder.getId());

        LocalDateTime firstInstanceTime = nextCheckInTime;

        ReminderInstance firstInstance = new ReminderInstance();
        firstInstance.setReminder(savedReminder);
        firstInstance.setSchedule(savedSchedule);
        firstInstance.setScheduledTime(firstInstanceTime);
        firstInstance.setResponseDeadline(firstInstanceTime.plusMinutes(180));
        firstInstance.setNextRemindAt(firstInstanceTime);
        firstInstance.setStatus(com.example.reminder.domain.enums.ReminderInstanceStatus.PENDING);
        firstInstance.setEscalationLevel(0);
        firstInstance.setMissedCount(0);

        reminderInstanceRepository.save(firstInstance);
        log.info("Created first instance for daily reminder at: {}", firstInstanceTime);
    }

    @Override
    @Transactional
    public void updateDailyCheckInTime(Long userId, LocalTime checkInTime) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        LocalTime normalized = checkInTime == null ? LocalTime.of(20, 0) : checkInTime.withSecond(0).withNano(0);
        user.setDailyCheckInTime(normalized);
        userRepository.save(user);

        Reminder reminder = reminderRepository.findFirstByUserIdAndSourceTypeAndDeletedAtIsNullOrderByCreatedAtAsc(userId, ReminderSourceType.SYSTEM)
                .orElseGet(() -> {
                    createDailyCheckInReminder(userId);
                    return reminderRepository.findFirstByUserIdAndSourceTypeAndDeletedAtIsNullOrderByCreatedAtAsc(userId, ReminderSourceType.SYSTEM)
                            .orElseThrow(() -> new ResourceNotFoundException("Daily check-in reminder not found for user: " + userId));
                });

        ReminderSchedule schedule = reminderScheduleRepository.findByReminderIdAndDeletedAtIsNull(reminder.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Daily check-in schedule not found for user: " + userId));

        LocalDateTime nextTime = nextOccurrence(normalized);
        schedule.setStartDatetime(nextTime);
        schedule.setUpdatedAt(LocalDateTime.now());
        reminderScheduleRepository.save(schedule);

        reminderInstanceRepository.softDeleteFutureInstancesByReminderId(reminder.getId(), LocalDateTime.now(), LocalDateTime.now());
        ReminderInstance nextInstance = new ReminderInstance();
        nextInstance.setReminder(reminder);
        nextInstance.setSchedule(schedule);
        nextInstance.setScheduledTime(nextTime);
        nextInstance.setResponseDeadline(nextTime.plusMinutes(180));
        nextInstance.setNextRemindAt(nextTime);
        nextInstance.setStatus(com.example.reminder.domain.enums.ReminderInstanceStatus.PENDING);
        nextInstance.setEscalationLevel(0);
        nextInstance.setMissedCount(0);
        reminderInstanceRepository.save(nextInstance);
    }

    private LocalDateTime nextOccurrence(LocalTime checkInTime) {
        LocalTime time = checkInTime == null ? LocalTime.of(20, 0) : checkInTime.withSecond(0).withNano(0);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime candidate = now.toLocalDate().atTime(time);
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }
}
