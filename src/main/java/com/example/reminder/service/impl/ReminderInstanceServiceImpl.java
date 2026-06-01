package com.example.reminder.service.impl;

import com.example.reminder.domain.enums.DayOfWeek;
import com.example.reminder.domain.enums.ReminderInstanceStatus;
import com.example.reminder.domain.enums.ReminderStatus;
import com.example.reminder.domain.enums.RiskLevel;
import com.example.reminder.domain.enums.ScheduleType;
import com.example.reminder.dto.reminderinstance.ReminderInstanceResponseDto;
import com.example.reminder.dto.reminderinstance.TodayReminderScheduleDto;
import com.example.reminder.entity.Reminder;
import com.example.reminder.entity.ReminderInstance;
import com.example.reminder.entity.ReminderSchedule;
import com.example.reminder.entity.User;
import com.example.reminder.entity.UserSafetyState;
import com.example.reminder.exception.BadRequestException;
import com.example.reminder.exception.ForbiddenException;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.repository.ReminderInstanceRepository;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.ReminderScheduleRepository;
import com.example.reminder.service.ReminderInstanceService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.reminder.domain.enums.NotificationType;
import com.example.reminder.domain.enums.ReminderSourceType;
import com.example.reminder.domain.enums.UserResponseAction;
import com.example.reminder.entity.EscalationLog;
import com.example.reminder.entity.UserResponse;

@Service
@RequiredArgsConstructor
public class ReminderInstanceServiceImpl implements ReminderInstanceService {

    private static final int ROLLING_WINDOW_DAYS = 3;

    private final ReminderRepository reminderRepository;
    private final ReminderScheduleRepository reminderScheduleRepository;
    private final ReminderInstanceRepository reminderInstanceRepository;
    private final com.example.reminder.repository.UserResponseRepository userResponseRepository;
    private final com.example.reminder.repository.EscalationLogRepository escalationLogRepository;
    private final com.example.reminder.service.NotificationService notificationService;
    private final com.example.reminder.repository.UserSafetyStateRepository userSafetyStateRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ReminderInstanceResponseDto> getByReminderId(Long reminderId, Long requesterUserId, Pageable pageable) {
        Reminder reminder = getAccessibleReminder(reminderId, requesterUserId);
        return reminderInstanceRepository.findByReminderIdAndDeletedAtIsNull(reminder.getId(), pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional
    public void handleUserResponse(Long userId, Long instanceId, UserResponseAction action) {
        ReminderInstance instance = reminderInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new com.example.reminder.exception.ResourceNotFoundException("Reminder instance not found: " + instanceId));

        if (!instance.getReminder().getUser().getId().equals(userId)) {
            throw new com.example.reminder.exception.ForbiddenException("No permission to respond to this reminder instance");
        }

        if (instance.getReminder().getSourceType() == ReminderSourceType.USER) {
            throw new BadRequestException("User reminders do not accept responses");
        }

        if (instance.getStatus() != ReminderInstanceStatus.PENDING && instance.getStatus() != ReminderInstanceStatus.SNOOZED) {
            throw new BadRequestException("Only pending or snoozed reminder instances can be updated");
        }

        UserResponse response = new UserResponse();
        response.setReminderInstance(instance);
        response.setAction(action);
        response.setResponseTime(java.time.LocalDateTime.now());
        userResponseRepository.save(response);

        switch (action) {
            case IM_SAFE -> {
                instance.setStatus(ReminderInstanceStatus.COMPLETED);
                instance.setResolvedAt(java.time.LocalDateTime.now());
                instance.setNextRemindAt(null);
                reminderInstanceRepository.save(instance);
                updateUserSafetyStateOnSafe(instance.getReminder().getUser(), java.time.LocalDateTime.now());

                // add escalation log - STOP (only for system-created reminders)
                if (instance.getReminder().getSourceType() == ReminderSourceType.SYSTEM) {
                    EscalationLog log = new EscalationLog();
                    log.setReminderInstance(instance);
                    log.setLevel(instance.getEscalationLevel());
                    log.setNotificationType(NotificationType.BANNER);
                    log.setTriggeredAt(java.time.LocalDateTime.now());
                    escalationLogRepository.save(log);
                }
            }
            case SNOOZE -> {
                // schedule next remind 30 minutes later
                java.time.LocalDateTime next = java.time.LocalDateTime.now().plusMinutes(30);
                instance.setNextRemindAt(next);
                instance.setStatus(ReminderInstanceStatus.SNOOZED);
                instance.setLastNotificationAt(java.time.LocalDateTime.now());
                instance.setMissedCount(instance.getMissedCount());
                reminderInstanceRepository.save(instance);
            }
            default -> {
                // other actions currently just recorded
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ReminderInstanceResponseDto getById(Long reminderId, Long instanceId, Long requesterUserId) {
        Reminder reminder = getAccessibleReminder(reminderId, requesterUserId);
        ReminderInstance instance = reminderInstanceRepository.findByIdAndReminderIdAndDeletedAtIsNull(instanceId, reminder.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Reminder instance not found: " + instanceId));
        return toDto(instance);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TodayReminderScheduleDto> getTodaySchedules(Long requesterUserId) {
        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);

        return reminderInstanceRepository.findScheduledForUserBetween(
                        requesterUserId,
                        ReminderStatus.ACTIVE,
                        startOfToday,
                        startOfTomorrow
                )
                .stream()
                .map(this::toTodayDto)
                .toList();
    }

    @Override
    @Transactional
    public void syncRollingWindowForSchedule(Long scheduleId) {
        ReminderSchedule schedule = getActiveSchedule(scheduleId);
        LocalDateTime now = LocalDateTime.now();

        if (schedule.getReminder().getStatus() == ReminderStatus.ACTIVE) {
            createMissingInstances(schedule, schedule.getType() == ScheduleType.ONCE ? schedule.getStartDatetime() : now, now.plusDays(ROLLING_WINDOW_DAYS));
        }
    }

    @Override
    @Transactional
    public void softDeleteFutureInstancesForSchedule(Long scheduleId) {
        ReminderSchedule schedule = reminderScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + scheduleId));
        softDeleteFutureInstancesByScheduleId(schedule.getReminder().getId(), scheduleId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void softDeleteFutureInstancesForReminder(Long reminderId) {
        Reminder reminder = getAccessibleReminder(reminderId, null);
        reminderInstanceRepository.softDeleteFutureInstancesByReminderId(reminder.getId(), LocalDateTime.now(), LocalDateTime.now());
    }

    @Override
    @Transactional
    public void syncRollingWindowsForReminder(Long reminderId) {
        Reminder reminder = getActiveReminder(reminderId);
        if (reminder.getStatus() != ReminderStatus.ACTIVE) {
            return;
        }

        List<ReminderSchedule> schedules = reminderScheduleRepository.findByReminderIdAndDeletedAtIsNull(reminder.getId());
        for (ReminderSchedule schedule : schedules) {
            syncRollingWindowForSchedule(schedule.getId());
        }
    }

    @Override
    @Transactional
    public void refreshRollingWindowsForActiveReminders() {
        List<Reminder> reminders = reminderRepository.findByStatusAndDeletedAtIsNull(ReminderStatus.ACTIVE);
        for (Reminder reminder : reminders) {
            syncRollingWindowsForReminder(reminder.getId());
        }
    }

    private void softDeleteFutureInstancesByScheduleId(Long reminderId, Long scheduleId, LocalDateTime fromTime) {
        reminderInstanceRepository.softDeleteFutureInstancesByReminderIdAndScheduleId(reminderId, scheduleId, fromTime, LocalDateTime.now());
    }

    private Reminder getAccessibleReminder(Long reminderId, Long requesterUserId) {
        Reminder reminder = reminderRepository.findByIdAndDeletedAtIsNull(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found: " + reminderId));

        if (requesterUserId != null && !reminder.getUser().getId().equals(requesterUserId)) {
            throw new ForbiddenException("No permission to access this reminder instance");
        }

        return reminder;
    }

    private Reminder getActiveReminder(Long reminderId) {
        return reminderRepository.findByIdAndDeletedAtIsNull(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found: " + reminderId));
    }

    private ReminderSchedule getActiveSchedule(Long scheduleId) {
        return reminderScheduleRepository.findByIdAndDeletedAtIsNull(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + scheduleId));
    }

    private void createMissingInstances(ReminderSchedule schedule, LocalDateTime fromTime, LocalDateTime windowEnd) {
        LocalDateTime effectiveStart = fromTime.isAfter(schedule.getStartDatetime()) ? fromTime : schedule.getStartDatetime();
        LocalDateTime effectiveEnd = windowEnd;
        if (schedule.getEndDatetime() != null && schedule.getEndDatetime().isBefore(effectiveEnd)) {
            effectiveEnd = schedule.getEndDatetime();
        }

        if (effectiveStart.isAfter(effectiveEnd)) {
            return;
        }

        Set<LocalDateTime> existingTimes = new HashSet<>();
        reminderInstanceRepository
                .findByReminderIdAndScheduleIdAndDeletedAtIsNullAndScheduledTimeBetweenOrderByScheduledTimeAsc(
                        schedule.getReminder().getId(),
                        schedule.getId(),
                        effectiveStart,
                        effectiveEnd
                )
                .forEach(instance -> existingTimes.add(instance.getScheduledTime()));

        List<LocalDateTime> targetTimes = generateOccurrences(schedule, effectiveStart, effectiveEnd);
        List<ReminderInstance> instancesToCreate = new ArrayList<>();
        for (LocalDateTime scheduledTime : targetTimes) {
            if (existingTimes.contains(scheduledTime)) {
                continue;
            }

            ReminderInstance reminderInstance = new ReminderInstance();
            reminderInstance.setReminder(schedule.getReminder());
            reminderInstance.setSchedule(schedule);
            reminderInstance.setScheduledTime(scheduledTime);
            reminderInstance.setStatus(ReminderInstanceStatus.PENDING);
            reminderInstance.setEscalationLevel(0);
            reminderInstance.setMissedCount(0);
            if(schedule.getEndDatetime() != null){
                LocalDateTime responseDeadline = LocalDateTime.of(
                    scheduledTime.toLocalDate(),
                    schedule.getEndDatetime().toLocalTime()
                );
                reminderInstance.setResponseDeadline(responseDeadline);
            }
            if (schedule.getReminder().getSourceType() == ReminderSourceType.SYSTEM) {
                reminderInstance.setResponseDeadline(scheduledTime.plusHours(6));
            }
            instancesToCreate.add(reminderInstance);
        }

        instancesToCreate.sort(Comparator.comparing(ReminderInstance::getScheduledTime));
        if (!instancesToCreate.isEmpty()) {
            reminderInstanceRepository.saveAll(instancesToCreate);
        }
    }

    private List<LocalDateTime> generateOccurrences(ReminderSchedule schedule, LocalDateTime fromTime, LocalDateTime windowEnd) {
        List<LocalDateTime> occurrences = new ArrayList<>();
        ScheduleType type = schedule.getType();

        if (type == ScheduleType.ONCE) {
            LocalDateTime candidate = resolveOnceOccurrence(schedule);
            addIfWithinWindow(occurrences, candidate, fromTime, windowEnd, schedule.getEndDatetime());
            return occurrences;
        }

        if (hasSelectedDays(schedule)) {
            addDayBasedOccurrences(schedule, fromTime, windowEnd, occurrences);
            return occurrences;
        }

        int periodDays = switch (type) {
            case DAILY -> Math.max(1, schedule.getIntervalValue() == null ? 1 : schedule.getIntervalValue());
            case WEEKLY -> Math.max(1, schedule.getIntervalValue() == null ? 1 : schedule.getIntervalValue()) * 7;
            case CUSTOM -> Math.max(1, schedule.getIntervalValue() == null ? 1 : schedule.getIntervalValue());
            default -> 1;
        };

        LocalDateTime candidate = alignPeriodicStart(schedule.getStartDatetime(), fromTime, periodDays);
        while (!candidate.isAfter(windowEnd)) {
            if (schedule.getEndDatetime() != null && candidate.isAfter(schedule.getEndDatetime())) {
                break;
            }
            occurrences.add(candidate);
            candidate = candidate.plusDays(periodDays);
        }

        return occurrences;
    }

    private LocalDateTime resolveOnceOccurrence(ReminderSchedule schedule) {
        if (!hasSelectedDays(schedule)) {
            return schedule.getStartDatetime();
        }

        java.time.DayOfWeek selectedDay = toJavaDayOfWeek(schedule.getDaysOfWeek().iterator().next());
        LocalDateTime startDatetime = schedule.getStartDatetime();
        int daysUntilSelectedDay = (selectedDay.getValue() - startDatetime.getDayOfWeek().getValue() + 7) % 7;

        return startDatetime.plusDays(daysUntilSelectedDay);
    }

    private void addDayBasedOccurrences(
            ReminderSchedule schedule,
            LocalDateTime fromTime,
            LocalDateTime windowEnd,
            List<LocalDateTime> occurrences
    ) {
        Set<java.time.DayOfWeek> activeDays = schedule.getDaysOfWeek().stream()
                .map(this::toJavaDayOfWeek)
                .collect(Collectors.toSet());
        int weekInterval = Math.max(1, schedule.getIntervalValue() == null ? 1 : schedule.getIntervalValue());
        LocalDate startDate = fromTime.toLocalDate();
        LocalDate endDate = windowEnd.toLocalDate();
        LocalDate baseDate = schedule.getStartDatetime().toLocalDate();
        java.time.LocalTime baseTime = schedule.getStartDatetime().toLocalTime();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (!activeDays.contains(date.getDayOfWeek())) {
                continue;
            }

            long weeksBetween = ChronoUnit.WEEKS.between(baseDate, date);
            if (weeksBetween % weekInterval != 0) {
                continue;
            }

            LocalDateTime candidate = LocalDateTime.of(date, baseTime);
            if (candidate.isBefore(fromTime)) {
                continue;
            }
            if (schedule.getEndDatetime() != null && candidate.isAfter(schedule.getEndDatetime())) {
                continue;
            }
            if (!candidate.isAfter(windowEnd)) {
                occurrences.add(candidate);
            }
        }
    }

    private void addIfWithinWindow(
            List<LocalDateTime> occurrences,
            LocalDateTime candidate,
            LocalDateTime fromTime,
            LocalDateTime windowEnd,
            LocalDateTime scheduleEnd
    ) {
        if (candidate.isBefore(fromTime) || candidate.isAfter(windowEnd)) {
            return;
        }
        if (scheduleEnd != null && candidate.isAfter(scheduleEnd)) {
            return;
        }
        occurrences.add(candidate);
    }

    private void updateUserSafetyStateOnSafe(User user, LocalDateTime now) {
        UserSafetyState safetyState = userSafetyStateRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserSafetyState created = new UserSafetyState();
                    created.setUser(user);
                    created.setCreatedAt(now);
                    return created;
                });

        safetyState.setLastCheckinAt(now);
        safetyState.setConsecutiveMissedCount(0);
        safetyState.setRiskLevel(RiskLevel.LOW);
        safetyState.setUpdatedAt(now);
        userSafetyStateRepository.save(safetyState);
    }

    private boolean hasSelectedDays(ReminderSchedule schedule) {
        return schedule.getDaysOfWeek() != null && !schedule.getDaysOfWeek().isEmpty();
    }

    private LocalDateTime alignPeriodicStart(LocalDateTime base, LocalDateTime cursor, int periodDays) {
        if (!base.isBefore(cursor)) {
            return base;
        }

        long daysBetween = ChronoUnit.DAYS.between(base.toLocalDate(), cursor.toLocalDate());
        long steps = daysBetween / periodDays;
        LocalDateTime candidate = base.plusDays(steps * periodDays);
        while (candidate.isBefore(cursor)) {
            candidate = candidate.plusDays(periodDays);
        }
        return candidate;
    }

    private java.time.DayOfWeek toJavaDayOfWeek(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MON -> java.time.DayOfWeek.MONDAY;
            case TUE -> java.time.DayOfWeek.TUESDAY;
            case WED -> java.time.DayOfWeek.WEDNESDAY;
            case THU -> java.time.DayOfWeek.THURSDAY;
            case FRI -> java.time.DayOfWeek.FRIDAY;
            case SAT -> java.time.DayOfWeek.SATURDAY;
            case SUN -> java.time.DayOfWeek.SUNDAY;
        };
    }

    private ReminderInstanceResponseDto toDto(ReminderInstance instance) {
        return new ReminderInstanceResponseDto(
                instance.getId(),
                instance.getReminder().getId(),
                instance.getSchedule() == null ? null : instance.getSchedule().getId(),
                instance.getScheduledTime(),
                instance.getStatus(),
                instance.getEscalationLevel(),
                instance.getMissedCount(),
                instance.getLastNotificationAt(),
                instance.getResolvedAt(),
                instance.getDeletedAt()
        );
    }

    private TodayReminderScheduleDto toTodayDto(ReminderInstance instance) {
        ReminderSchedule schedule = instance.getSchedule();
        return new TodayReminderScheduleDto(
                instance.getId(),
                instance.getReminder().getId(),
                schedule == null ? null : schedule.getId(),
                instance.getReminder().getTitle(),
                instance.getReminder().getDescription(),
                schedule == null ? null : schedule.getType(),
                schedule == null ? Set.of() : schedule.getDaysOfWeek(),
                instance.getScheduledTime(),
                instance.getStatus(),
                instance.getEscalationLevel(),
                instance.getMissedCount()
        );
    }
}