package com.example.reminder.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.reminder.domain.enums.TonePreference;
import com.example.reminder.domain.enums.UserStatus;
import com.example.reminder.entity.Reminder;
import com.example.reminder.entity.ReminderInstance;
import com.example.reminder.entity.ReminderSchedule;
import com.example.reminder.entity.User;
import com.example.reminder.entity.UserSafetyState;
import com.example.reminder.repository.ReminderInstanceRepository;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.ReminderScheduleRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.repository.UserSafetyStateRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DailyReminderServiceImplTest {

    private final ReminderRepository reminderRepository = mock(ReminderRepository.class);
    private final ReminderScheduleRepository reminderScheduleRepository = mock(ReminderScheduleRepository.class);
    private final ReminderInstanceRepository reminderInstanceRepository = mock(ReminderInstanceRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserSafetyStateRepository userSafetyStateRepository = mock(UserSafetyStateRepository.class);

    private final DailyReminderServiceImpl service = new DailyReminderServiceImpl(
            reminderRepository,
            reminderScheduleRepository,
            reminderInstanceRepository,
            userRepository,
            userSafetyStateRepository
    );

    @Test
    void createDailyCheckInReminder_setsStartTimeAt20AndDeadlineAt2NextDay() {
        User user = new User();
        user.setId(42L);
        user.setEmail("new@example.com");
        user.setFullName("New User");
        user.setStatus(UserStatus.ACTIVE);
        user.setTonePreference(TonePreference.NORMAL);

        when(userRepository.findByIdAndDeletedAtIsNull(42L)).thenReturn(Optional.of(user));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reminderScheduleRepository.save(any(ReminderSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reminderInstanceRepository.save(any(ReminderInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userSafetyStateRepository.save(any(UserSafetyState.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createDailyCheckInReminder(42L);

        ArgumentCaptor<ReminderSchedule> scheduleCaptor = ArgumentCaptor.forClass(ReminderSchedule.class);
        ArgumentCaptor<ReminderInstance> instanceCaptor = ArgumentCaptor.forClass(ReminderInstance.class);

        verify(reminderScheduleRepository).save(scheduleCaptor.capture());
        verify(reminderInstanceRepository).save(instanceCaptor.capture());

        ReminderSchedule schedule = scheduleCaptor.getValue();
        ReminderInstance instance = instanceCaptor.getValue();

        assertNotNull(schedule.getStartDatetime());
        assertEquals(20, schedule.getStartDatetime().getHour());
        assertEquals(0, schedule.getStartDatetime().getMinute());
        assertEquals(schedule.getStartDatetime(), instance.getScheduledTime());
        assertEquals(2, instance.getResponseDeadline().getHour());
        assertEquals(0, instance.getResponseDeadline().getMinute());
        assertEquals(instance.getScheduledTime().plusHours(6), instance.getResponseDeadline());
    }
}
