package com.example.reminder.service.impl;

import com.example.reminder.domain.enums.ReminderSourceType;
import com.example.reminder.domain.enums.ReminderStatus;
import com.example.reminder.domain.enums.RiskLevel;
import com.example.reminder.domain.enums.ScheduleType;
import com.example.reminder.domain.enums.TonePreference;
import com.example.reminder.entity.Reminder;
import com.example.reminder.entity.ReminderInstance;
import com.example.reminder.entity.ReminderSchedule;
import com.example.reminder.entity.User;
import com.example.reminder.entity.UserSafetyState;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.repository.ReminderInstanceRepository;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.ReminderScheduleRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.repository.UserSafetyStateRepository;
import com.example.reminder.service.DailyReminderService;
import java.time.LocalDateTime;
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
    private final UserSafetyStateRepository userSafetyStateRepository;

    @Override
    @Transactional
    public void createDailyCheckInReminder(Long userId) {
        // Get user
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Create system reminder
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

        // Create schedule (DAILY starting from now)
        ReminderSchedule schedule = new ReminderSchedule();
        schedule.setReminder(savedReminder);
        schedule.setType(ScheduleType.DAILY);
        schedule.setIntervalValue(1);
        schedule.setStartDatetime(LocalDateTime.now());
        schedule.setEndDatetime(null); // No end date
        schedule.setCreatedAt(LocalDateTime.now());

        ReminderSchedule savedSchedule = reminderScheduleRepository.save(schedule);
        log.info("Created daily schedule for reminder: {}", savedReminder.getId());

        // Create the first instance (scheduled for today at current time + 1 day)
        LocalDateTime firstInstanceTime = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        
        ReminderInstance firstInstance = new ReminderInstance();
        firstInstance.setReminder(savedReminder);
        firstInstance.setSchedule(savedSchedule);
        firstInstance.setScheduledTime(firstInstanceTime);
        firstInstance.setResponseDeadline(firstInstanceTime.plusHours(23)); // 23 hours to respond
        firstInstance.setStatus(com.example.reminder.domain.enums.ReminderInstanceStatus.PENDING);
        firstInstance.setEscalationLevel(0);
        firstInstance.setMissedCount(0);

        reminderInstanceRepository.save(firstInstance);
        log.info("Created first instance for daily reminder at: {}", firstInstanceTime);

        // Initialize user safety state
        UserSafetyState safetyState = new UserSafetyState();
        safetyState.setUser(user);
        safetyState.setConsecutiveMissedCount(0);
        safetyState.setRiskLevel(RiskLevel.LOW);
        safetyState.setCreatedAt(LocalDateTime.now());

        userSafetyStateRepository.save(safetyState);
        log.info("Initialized safety state for user: {}", userId);
    }
}
