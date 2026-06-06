package com.example.reminder.service.impl;

import com.example.reminder.domain.enums.ActivityLogType;
import com.example.reminder.domain.enums.DayOfWeek;
import com.example.reminder.domain.enums.ReminderInstanceStatus;
import com.example.reminder.domain.enums.ReminderSourceType;
import com.example.reminder.domain.enums.ReminderStatus;
import com.example.reminder.domain.enums.ScheduleType;
import com.example.reminder.domain.enums.UserResponseAction;
import com.example.reminder.dto.reminderinstance.ReminderInstanceResponseDto;
import com.example.reminder.dto.reminderinstance.TodayReminderScheduleDto;
import com.example.reminder.entity.Reminder;
import com.example.reminder.entity.ReminderInstance;
import com.example.reminder.entity.ReminderSchedule;
import com.example.reminder.entity.User;
import com.example.reminder.entity.UserResponse;
import com.example.reminder.exception.BadRequestException;
import com.example.reminder.exception.ForbiddenException;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.repository.ReminderInstanceRepository;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.ReminderScheduleRepository;
import com.example.reminder.repository.UserResponseRepository;
import com.example.reminder.service.ActivityLogService;
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

@Service
@RequiredArgsConstructor
public class ReminderInstanceServiceImpl implements ReminderInstanceService {

    private static final int ROLLING_WINDOW_DAYS = 3;

    private final ReminderRepository reminderRepository;
    private final ReminderScheduleRepository reminderScheduleRepository;
    private final ReminderInstanceRepository reminderInstanceRepository;
    private final UserResponseRepository userResponseRepository;
    private final ActivityLogService activityLogService;

    @Override
    @Transactional(readOnly = true)
    public Page<ReminderInstanceResponseDto> getByReminderId(Long reminderId, Long requesterUserId, Pageable pageable) {
        Reminder reminder = getAccessibleReminder(reminderId, requesterUserId);
        Page<ReminderInstance> page = isFreeDailyCheckIn(reminder)
                ? reminderInstanceRepository.findByReminderIdAndDeletedAtIsNullAndScheduledTimeAfter(
                        reminder.getId(),
                        LocalDateTime.now().minusDays(3),
                        pageable
                )
                : reminderInstanceRepository.findByReminderIdAndDeletedAtIsNull(reminder.getId(), pageable);
        return page
                .map(this::toDto);
    }

    @Override
    @Transactional
    public ReminderInstanceResponseDto respond(Long instanceId, Long requesterUserId, UserResponseAction action, String payload) {
        ReminderInstance instance = reminderInstanceRepository.findById(instanceId)
                .filter(item -> item.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lần nhắc: " + instanceId));

        if (!instance.getReminder().getUser().getId().equals(requesterUserId)) {
            throw new ForbiddenException("Bạn không có quyền phản hồi lần nhắc này");
        }

        if (instance.getStatus() == ReminderInstanceStatus.MISSED
                || instance.getStatus() == ReminderInstanceStatus.COMPLETED
                || instance.getStatus() == ReminderInstanceStatus.DONE) {
            throw new BadRequestException("Lần nhắc này đã kết thúc nên không thể check-in nữa");
        }

        UserResponse response = new UserResponse();
        response.setReminderInstance(instance);
        response.setAction(action);
        response.setPayload(payload);
        response.setResponseTime(LocalDateTime.now());
        userResponseRepository.save(response);

        if (action == UserResponseAction.IM_SAFE) {
            instance.setStatus(ReminderInstanceStatus.DONE);
            instance.setResolvedAt(LocalDateTime.now());
            instance.setNextRemindAt(null);
        } else if (action == UserResponseAction.SNOOZE) {
            instance.setStatus(ReminderInstanceStatus.SNOOZED);
            instance.setNextRemindAt(LocalDateTime.now().plusMinutes(5));
        } else if (action == UserResponseAction.NEED_HELP) {
            instance.setStatus(ReminderInstanceStatus.ESCALATED);
            instance.setEscalationLevel(Math.max(instance.getEscalationLevel() == null ? 0 : instance.getEscalationLevel(), 2));
            instance.setNextRemindAt(LocalDateTime.now());
        }

        ReminderInstance saved = reminderInstanceRepository.save(instance);
        recordResponseActivity(saved, action);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void handleUserResponse(Long userId, Long instanceId, UserResponseAction action) {
        respond(instanceId, userId, action, null);
    }

    @Override
    @Transactional(readOnly = true)
    public ReminderInstanceResponseDto getById(Long reminderId, Long instanceId, Long requesterUserId) {
        Reminder reminder = getAccessibleReminder(reminderId, requesterUserId);
        ReminderInstance instance = reminderInstanceRepository.findByIdAndReminderIdAndDeletedAtIsNull(instanceId, reminder.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lần nhắc: " + instanceId));
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch nhắc: " + scheduleId));
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời nhắc: " + reminderId));

        if (requesterUserId != null && !reminder.getUser().getId().equals(requesterUserId)) {
            throw new ForbiddenException("Bạn không có quyền truy cập lần nhắc này");
        }

        return reminder;
    }

    private Reminder getActiveReminder(Long reminderId) {
        return reminderRepository.findByIdAndDeletedAtIsNull(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời nhắc: " + reminderId));
    }

    private ReminderSchedule getActiveSchedule(Long scheduleId) {
        return reminderScheduleRepository.findByIdAndDeletedAtIsNull(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch nhắc: " + scheduleId));
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
            reminderInstance.setResponseDeadline(scheduledTime.plusMinutes(180));
            reminderInstance.setNextRemindAt(scheduledTime);
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

    private boolean isFreeDailyCheckIn(Reminder reminder) {
        User user = reminder.getUser();
        String planName = user.getCurrentPlan() == null ? "FREE" : user.getCurrentPlan().getName();
        return reminder.getSourceType() == ReminderSourceType.SYSTEM
                && (planName == null || planName.equalsIgnoreCase("FREE") || planName.equalsIgnoreCase("FREEMIUM"));
    }

    private void recordResponseActivity(ReminderInstance instance, UserResponseAction action) {
        ActivityLogType type = switch (action) {
            case IM_SAFE -> ActivityLogType.CHECKED_IN;
            case SNOOZE -> ActivityLogType.CHECK_IN_SNOOZED;
            case NEED_HELP -> ActivityLogType.HELP_REQUESTED;
        };
        String title = switch (action) {
            case IM_SAFE -> "Đã check-in";
            case SNOOZE -> "Đã hoãn check-in";
            case NEED_HELP -> "Đã yêu cầu trợ giúp";
        };
        String message = switch (action) {
            case IM_SAFE -> "Bạn đã check-in cho \"" + instance.getReminder().getTitle() + "\".";
            case SNOOZE -> "Bạn đã hoãn check-in cho \"" + instance.getReminder().getTitle() + "\".";
            case NEED_HELP -> "Bạn đã yêu cầu trợ giúp cho \"" + instance.getReminder().getTitle() + "\".";
        };

        activityLogService.record(
                instance.getReminder().getUser().getId(),
                type,
                title,
                message,
                instance.getReminder().getId(),
                instance.getSchedule() == null ? null : instance.getSchedule().getId(),
                instance.getId(),
                "{\"action\":\"" + action + "\"}"
        );
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
