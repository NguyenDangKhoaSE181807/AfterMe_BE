package com.example.reminder.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.example.reminder.domain.enums.RiskLevel;
import com.example.reminder.domain.enums.ScheduleType;
import com.example.reminder.domain.enums.UserResponseAction;
import com.example.reminder.dto.reminderinstance.TodayReminderScheduleDto;
import com.example.reminder.entity.Reminder;
import com.example.reminder.entity.ReminderInstance;
import com.example.reminder.entity.ReminderSchedule;
import com.example.reminder.entity.UserSafetyState;
import com.example.reminder.exception.BadRequestException;
import com.example.reminder.repository.EscalationLogRepository;
import com.example.reminder.repository.ReminderInstanceRepository;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.ReminderScheduleRepository;
import com.example.reminder.repository.UserResponseRepository;
import com.example.reminder.repository.UserSafetyStateRepository;
import com.example.reminder.service.NotificationService;
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
        private final UserResponseRepository userResponseRepository = mock(UserResponseRepository.class);
        private final EscalationLogRepository escalationLogRepository = mock(EscalationLogRepository.class);
        private final NotificationService notificationService = mock(NotificationService.class);
        private final UserSafetyStateRepository userSafetyStateRepository = mock(UserSafetyStateRepository.class);
    private final ReminderInstanceServiceImpl service = new ReminderInstanceServiceImpl(
            reminderRepository,
            reminderScheduleRepository,
            reminderInstanceRepository,
            userResponseRepository,
            escalationLogRepository,
            notificationService,
            userSafetyStateRepository
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

    @Test
    void handleUserResponse_rejectsCompletedInstance() {
        ReminderInstance instance = createOwnedInstance(ReminderInstanceStatus.COMPLETED);
        when(reminderInstanceRepository.findById(30L)).thenReturn(Optional.of(instance));

        assertThrows(BadRequestException.class,
                () -> service.handleUserResponse(99L, 30L, UserResponseAction.SNOOZE));

        verify(userResponseRepository, never()).save(any());
        verify(reminderInstanceRepository, never()).save(any());
    }

    @Test
    void handleUserResponse_rejectsMissedInstance() {
        ReminderInstance instance = createOwnedInstance(ReminderInstanceStatus.MISSED);
        when(reminderInstanceRepository.findById(31L)).thenReturn(Optional.of(instance));

        assertThrows(BadRequestException.class,
                () -> service.handleUserResponse(99L, 31L, UserResponseAction.IM_SAFE));

        verify(userResponseRepository, never()).save(any());
        verify(reminderInstanceRepository, never()).save(any());
    }

    @Test
    void handleUserResponse_allowsPendingInstance() {
        ReminderInstance instance = createOwnedInstance(ReminderInstanceStatus.PENDING);
        when(reminderInstanceRepository.findById(32L)).thenReturn(Optional.of(instance));

        service.handleUserResponse(99L, 32L, UserResponseAction.SNOOZE);

        verify(userResponseRepository).save(any());
        verify(reminderInstanceRepository).save(any());
    }

    @Test
    void handleUserResponse_imSafeResetsSafetyState() {
        ReminderInstance instance = createOwnedInstance(ReminderInstanceStatus.PENDING);
        instance.getReminder().setSourceType(com.example.reminder.domain.enums.ReminderSourceType.SYSTEM);
        when(reminderInstanceRepository.findById(33L)).thenReturn(Optional.of(instance));

        UserSafetyState existingState = new UserSafetyState();
        existingState.setId(7L);
        existingState.setUser(instance.getReminder().getUser());
        existingState.setConsecutiveMissedCount(2);
        existingState.setRiskLevel(RiskLevel.HIGH);
        existingState.setCreatedAt(LocalDateTime.now().minusDays(2));

        when(userSafetyStateRepository.findByUserId(99L)).thenReturn(Optional.of(existingState));

        service.handleUserResponse(99L, 33L, UserResponseAction.IM_SAFE);

        assertEquals(0, existingState.getConsecutiveMissedCount());
        assertEquals(RiskLevel.LOW, existingState.getRiskLevel());
        assertNotNull(existingState.getLastCheckinAt());
        assertNotNull(existingState.getUpdatedAt());
        verify(userSafetyStateRepository).save(existingState);
    }

    private ReminderInstance createOwnedInstance(ReminderInstanceStatus status) {
        Reminder reminder = new Reminder();
        reminder.setId(10L);

        com.example.reminder.entity.User owner = new com.example.reminder.entity.User();
        owner.setId(99L);
        reminder.setUser(owner);

        ReminderInstance instance = new ReminderInstance();
        instance.setId(30L);
        instance.setReminder(reminder);
        instance.setStatus(status);
        instance.setEscalationLevel(0);
        instance.setMissedCount(0);
        return instance;
    }
}