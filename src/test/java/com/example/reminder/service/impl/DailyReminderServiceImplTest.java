package com.example.reminder.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.example.reminder.repository.ReminderInstanceRepository;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.ReminderScheduleRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.ActivityLogService;
import com.example.reminder.service.PassiveActivityService;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class DailyReminderServiceImplTest {

    private final ReminderRepository reminderRepository = mock(ReminderRepository.class);
    private final ReminderScheduleRepository reminderScheduleRepository = mock(ReminderScheduleRepository.class);
    private final ReminderInstanceRepository reminderInstanceRepository = mock(ReminderInstanceRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ActivityLogService activityLogService = mock(ActivityLogService.class);
    private final PassiveActivityService passiveActivityService = mock(PassiveActivityService.class);

    private final DailyReminderServiceImpl service = new DailyReminderServiceImpl(
            reminderRepository,
            reminderScheduleRepository,
            reminderInstanceRepository,
            userRepository,
            activityLogService,
            passiveActivityService
    );

    @Test
    void createDailyCheckInReminder_setsStartTimeAt20AndDeadlineThreeHoursLater() {
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
        assertEquals(23, instance.getResponseDeadline().getHour());
        assertEquals(0, instance.getResponseDeadline().getMinute());
        assertEquals(instance.getScheduledTime().plusMinutes(180), instance.getResponseDeadline());
    }

    @Test
    void calculateNextCheckInPlan_usesSameDayWhenNewTimeIsAtLeastEightHoursAfterLastCheckIn() throws Exception {
        Object plan = ReflectionTestUtils.invokeMethod(
                service,
                "calculateNextCheckInPlan",
                LocalTime.of(12, 0),
                LocalDateTime.of(2026, 6, 16, 1, 0),
                LocalDateTime.of(2026, 6, 16, 1, 5)
        );

        assertEquals(LocalDateTime.of(2026, 6, 16, 12, 0), regularTime(plan));
        assertTrue(transitionTime(plan).isEmpty());
    }

    @Test
    void calculateNextCheckInPlan_createsTransitionWhenTodayIsTooNearAndTomorrowIsTooFar() throws Exception {
        Object plan = ReflectionTestUtils.invokeMethod(
                service,
                "calculateNextCheckInPlan",
                LocalTime.of(6, 0),
                LocalDateTime.of(2026, 6, 16, 1, 0),
                LocalDateTime.of(2026, 6, 16, 1, 5)
        );

        assertEquals(LocalDateTime.of(2026, 6, 17, 6, 0), regularTime(plan));
        assertEquals(Optional.of(LocalDateTime.of(2026, 6, 16, 9, 0)), transitionTime(plan));
    }

    @Test
    void calculateNextCheckInPlan_truncatesTransitionSecondsToMinute() throws Exception {
        Object plan = ReflectionTestUtils.invokeMethod(
                service,
                "calculateNextCheckInPlan",
                LocalTime.of(6, 0),
                LocalDateTime.of(2026, 6, 16, 1, 0, 30),
                LocalDateTime.of(2026, 6, 16, 1, 5)
        );

        assertEquals(Optional.of(LocalDateTime.of(2026, 6, 16, 9, 0)), transitionTime(plan));
    }

    @Test
    void calculateNextCheckInPlan_usesTomorrowWhenTodayIsTooNearButTomorrowIsWithinMaxGap() throws Exception {
        Object plan = ReflectionTestUtils.invokeMethod(
                service,
                "calculateNextCheckInPlan",
                LocalTime.of(2, 0),
                LocalDateTime.of(2026, 6, 16, 1, 0),
                LocalDateTime.of(2026, 6, 16, 1, 5)
        );

        assertEquals(LocalDateTime.of(2026, 6, 17, 2, 0), regularTime(plan));
        assertTrue(transitionTime(plan).isEmpty());
    }

    @Test
    void calculateNextCheckInPlan_usesTomorrowWhenGapIsExactlyTwentyEightHours() throws Exception {
        Object plan = ReflectionTestUtils.invokeMethod(
                service,
                "calculateNextCheckInPlan",
                LocalTime.of(5, 0),
                LocalDateTime.of(2026, 6, 16, 1, 0),
                LocalDateTime.of(2026, 6, 16, 1, 5)
        );

        assertEquals(LocalDateTime.of(2026, 6, 17, 5, 0), regularTime(plan));
        assertTrue(transitionTime(plan).isEmpty());
    }

    @Test
    void isNightRisk_dependsOnExpectedMissedTime() {
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(service, "isNightRisk", LocalTime.of(21, 0)));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(service, "isNightRisk", LocalTime.of(3, 0)));
    }

    private LocalDateTime regularTime(Object plan) throws Exception {
        var method = plan.getClass().getDeclaredMethod("regularTime");
        method.setAccessible(true);
        return (LocalDateTime) method.invoke(plan);
    }

    @SuppressWarnings("unchecked")
    private Optional<LocalDateTime> transitionTime(Object plan) throws Exception {
        var method = plan.getClass().getDeclaredMethod("transitionTime");
        method.setAccessible(true);
        return (Optional<LocalDateTime>) method.invoke(plan);
    }
}
