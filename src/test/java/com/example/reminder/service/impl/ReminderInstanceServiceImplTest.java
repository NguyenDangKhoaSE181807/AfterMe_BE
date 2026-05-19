package com.example.reminder.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.reminder.domain.enums.DayOfWeek;
import com.example.reminder.domain.enums.ReminderInstanceStatus;
import com.example.reminder.domain.enums.ReminderStatus;
import com.example.reminder.domain.enums.ScheduleType;
import com.example.reminder.dto.reminderinstance.TodayReminderScheduleDto;
import com.example.reminder.entity.Reminder;
import com.example.reminder.entity.ReminderInstance;
import com.example.reminder.entity.ReminderSchedule;
import com.example.reminder.repository.ReminderInstanceRepository;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.ReminderScheduleRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class ReminderInstanceServiceImplTest {

    private final ReminderRepository reminderRepository = mock(ReminderRepository.class);
    private final ReminderScheduleRepository reminderScheduleRepository = mock(ReminderScheduleRepository.class);
    private final ReminderInstanceRepository reminderInstanceRepository = mock(ReminderInstanceRepository.class);
    private final ReminderInstanceServiceImpl service = new ReminderInstanceServiceImpl(
            reminderRepository,
            reminderScheduleRepository,
            reminderInstanceRepository
    );

    @Test
    void syncRollingWindowForSchedule_refreshesAdditivelyWithoutSoftDelete() {
        Reminder reminder = new Reminder();
        reminder.setId(10L);
        reminder.setStatus(ReminderStatus.ACTIVE);

        ReminderSchedule schedule = new ReminderSchedule();
        schedule.setId(20L);
        schedule.setReminder(reminder);
        schedule.setType(ScheduleType.DAILY);
        schedule.setStartDatetime(LocalDateTime.now().minusDays(1));

        ReminderInstance existing = new ReminderInstance();
        existing.setScheduledTime(LocalDateTime.now().withNano(0));

        when(reminderScheduleRepository.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(schedule));
        when(reminderInstanceRepository.findByReminderIdAndScheduleIdAndDeletedAtIsNullAndScheduledTimeBetweenOrderByScheduledTimeAsc(
                anyLong(), anyLong(), any(), any()))
            .thenReturn(List.of(existing));

        service.syncRollingWindowForSchedule(20L);

        verify(reminderInstanceRepository, never())
                .softDeleteFutureInstancesByReminderIdAndScheduleId(anyLong(), anyLong(), any(), any());
    }

    @Test
    void generateOccurrences_onceScheduleWithSelectedDay_usesNextMatchingWeekday() {
        ReminderSchedule schedule = new ReminderSchedule();
        schedule.setType(ScheduleType.ONCE);
        schedule.setStartDatetime(LocalDateTime.of(2026, 5, 6, 12, 56));
        schedule.setDaysOfWeek(Set.of(DayOfWeek.MON));

        List<LocalDateTime> occurrences = ReflectionTestUtils.invokeMethod(
                service,
                "generateOccurrences",
                schedule,
                LocalDateTime.of(2026, 5, 6, 12, 56),
                LocalDateTime.of(2026, 5, 20, 12, 56)
        );

        assertEquals(1, occurrences.size());
        assertEquals(LocalDateTime.of(2026, 5, 11, 12, 56), occurrences.get(0));
    }

    @Test
    void getTodaySchedules_returnsActiveInstancesForCurrentUserOnly() {
        Reminder reminder = new Reminder();
        reminder.setId(10L);
        reminder.setTitle("Daily check-in");
        reminder.setDescription("Confirm you are safe");
        reminder.setStatus(ReminderStatus.ACTIVE);

        ReminderSchedule schedule = new ReminderSchedule();
        schedule.setId(20L);
        schedule.setReminder(reminder);
        schedule.setType(ScheduleType.DAILY);
        schedule.setDaysOfWeek(Set.of());

        ReminderInstance instance = new ReminderInstance();
        instance.setId(30L);
        instance.setReminder(reminder);
        instance.setSchedule(schedule);
        instance.setScheduledTime(LocalDateTime.now().withNano(0));
        instance.setStatus(ReminderInstanceStatus.PENDING);
        instance.setEscalationLevel(1);
        instance.setMissedCount(2);

        when(reminderInstanceRepository.findScheduledForUserBetween(eq(99L), eq(ReminderStatus.ACTIVE), any(), any()))
                .thenReturn(List.of(instance));

        List<TodayReminderScheduleDto> result = service.getTodaySchedules(99L);

        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(reminderInstanceRepository).findScheduledForUserBetween(eq(99L), eq(ReminderStatus.ACTIVE), startCaptor.capture(), endCaptor.capture());

        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();
        assertEquals(startOfToday, startCaptor.getValue());
        assertEquals(startOfToday.plusDays(1), endCaptor.getValue());
        assertEquals(1, result.size());

        TodayReminderScheduleDto dto = result.get(0);
        assertEquals(30L, dto.instanceId());
        assertEquals(10L, dto.reminderId());
        assertEquals(20L, dto.scheduleId());
        assertEquals("Daily check-in", dto.reminderTitle());
        assertEquals("Confirm you are safe", dto.reminderDescription());
        assertEquals(ScheduleType.DAILY, dto.scheduleType());
        assertNotNull(dto.daysOfWeek());
        assertEquals(ReminderInstanceStatus.PENDING, dto.status());
        assertEquals(1, dto.escalationLevel());
        assertEquals(2, dto.missedCount());
    }
}