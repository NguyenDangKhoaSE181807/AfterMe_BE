package com.example.reminder.service.impl;

import com.example.reminder.domain.enums.ReminderStatus;
import com.example.reminder.domain.enums.ScheduleType;
import com.example.reminder.dto.reminder.CreateReminderScheduleRequest;
import com.example.reminder.dto.reminder.ReminderScheduleResponseDto;
import com.example.reminder.dto.reminder.UpdateReminderScheduleRequest;
import com.example.reminder.entity.Reminder;
import com.example.reminder.entity.ReminderSchedule;
import com.example.reminder.exception.ForbiddenException;
import com.example.reminder.exception.BadRequestException;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.ReminderScheduleRepository;
import com.example.reminder.service.ReminderInstanceService;
import com.example.reminder.service.ReminderScheduleService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReminderScheduleServiceImpl implements ReminderScheduleService {

    private final ReminderScheduleRepository reminderScheduleRepository;
    private final ReminderRepository reminderRepository;
    private final ReminderInstanceService reminderInstanceService;

    @Override
    @Transactional
    public ReminderScheduleResponseDto create(Long reminderId, Long requesterUserId, CreateReminderScheduleRequest request) {
        Reminder reminder = getAccessibleReminder(reminderId, requesterUserId);
        if (reminder.getStatus() == ReminderStatus.ARCHIVED) {
            throw new ForbiddenException("Cannot create schedule for archived reminder");
        }

        ReminderSchedule schedule = new ReminderSchedule();
        schedule.setReminder(reminder);
        schedule.setStartDatetime(request.startDatetime());
        schedule.setEndDatetime(request.endDatetime());
        schedule.setCreatedAt(LocalDateTime.now());

        // Validate request according to schedule type and apply fields
        applyAndValidateScheduleFields(schedule, request.type(), request.intervalValue(), request.daysOfWeek(), request.startDatetime(), request.endDatetime());

        ReminderSchedule saved = reminderScheduleRepository.save(schedule);
        reminderInstanceService.syncRollingWindowForSchedule(saved.getId());
        return toDto(saved);
    }

    @Override
    @Transactional
    public ReminderScheduleResponseDto update(Long reminderId, Long scheduleId, Long requesterUserId, UpdateReminderScheduleRequest request) {
        ReminderSchedule schedule = reminderScheduleRepository.findByIdAndDeletedAtIsNull(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + scheduleId));

        validateScheduleBelongsToReminder(schedule, reminderId);
        validateOwner(schedule.getReminder(), requesterUserId);
        if (schedule.getReminder().getStatus() == ReminderStatus.ARCHIVED) {
            throw new ForbiddenException("Cannot update schedule for archived reminder");
        }

        applyAndValidateScheduleFields(schedule, request.type(), request.intervalValue(), request.daysOfWeek(), request.startDatetime(), request.endDatetime());
        schedule.setUpdatedAt(LocalDateTime.now());

        ReminderSchedule updated = reminderScheduleRepository.save(schedule);
        reminderInstanceService.softDeleteFutureInstancesForSchedule(updated.getId());
        reminderInstanceService.syncRollingWindowForSchedule(updated.getId());
        return toDto(updated);
    }

    private void applyAndValidateScheduleFields(ReminderSchedule schedule,
                                                ScheduleType type,
                                                Integer intervalValue,
                                                Set<com.example.reminder.domain.enums.DayOfWeek> daysOfWeek,
                                                LocalDateTime startDatetime,
                                                LocalDateTime endDatetime) {
        // Normalize daysOfWeek
        Set<com.example.reminder.domain.enums.DayOfWeek> days = (daysOfWeek == null) ? Set.of() : daysOfWeek;

        // Special-case: if client provided all 7 days, treat as DAILY
        if (days.size() == 7) {
            type = ScheduleType.DAILY;
            days = Set.of();
        }

        // Validate per type
        switch (type) {
            case ONCE -> {
                if (intervalValue != null) {
                    throw new BadRequestException("intervalValue must not be provided for ONCE schedules");
                }
                if (startDatetime == null) {
                    throw new BadRequestException("startDatetime is required for ONCE schedules");
                }
                if (days.size() > 1) {
                    throw new BadRequestException("daysOfWeek for ONCE schedule may contain at most one day");
                }
                schedule.setType(ScheduleType.ONCE);
                schedule.setIntervalValue(null);
                schedule.setDaysOfWeek(days);
                schedule.setStartDatetime(startDatetime);
                schedule.setEndDatetime(endDatetime);
            }
            case DAILY -> {
                if (intervalValue != null) {
                    throw new BadRequestException("intervalValue must not be provided for DAILY schedules");
                }
                if (days != null && !days.isEmpty()) {
                    throw new BadRequestException("daysOfWeek must not be provided for DAILY schedules");
                }
                if (startDatetime == null) {
                    throw new BadRequestException("startDatetime is required for DAILY schedules");
                }
                schedule.setType(ScheduleType.DAILY);
                schedule.setIntervalValue(null);
                schedule.setDaysOfWeek(Set.of());
                schedule.setStartDatetime(startDatetime);
                schedule.setEndDatetime(endDatetime);
            }
            case WEEKLY -> {
                if (intervalValue != null) {
                    throw new BadRequestException("intervalValue must not be provided for WEEKLY schedules");
                }
                if (startDatetime == null) {
                    throw new BadRequestException("startDatetime is required for WEEKLY schedules");
                }
                if (days == null || days.isEmpty()) {
                    // If client did not provide daysOfWeek, default to the weekday of startDatetime
                    com.example.reminder.domain.enums.DayOfWeek defaultDay = com.example.reminder.domain.enums.DayOfWeek.fromJavaDay(startDatetime.getDayOfWeek());
                    schedule.setDaysOfWeek(Set.of(defaultDay));
                } else {
                    schedule.setDaysOfWeek(days);
                }
                schedule.setType(ScheduleType.WEEKLY);
                schedule.setIntervalValue(null);
                schedule.setStartDatetime(startDatetime);
                schedule.setEndDatetime(endDatetime);
            }
            case CUSTOM -> {
                if (intervalValue == null || intervalValue <= 0) {
                    throw new BadRequestException("intervalValue is required and must be > 0 for CUSTOM schedules");
                }
                if (days != null && !days.isEmpty()) {
                    throw new BadRequestException("daysOfWeek must not be provided for CUSTOM schedules");
                }
                if (startDatetime == null) {
                    throw new BadRequestException("startDatetime is required for CUSTOM schedules");
                }
                schedule.setType(ScheduleType.CUSTOM);
                schedule.setIntervalValue(intervalValue);
                schedule.setDaysOfWeek(Set.of());
                schedule.setStartDatetime(startDatetime);
                schedule.setEndDatetime(endDatetime);
            }
            default -> throw new BadRequestException("Unsupported schedule type: " + type);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ReminderScheduleResponseDto getById(Long reminderId, Long scheduleId, Long requesterUserId) {
        ReminderSchedule schedule = reminderScheduleRepository.findByIdAndDeletedAtIsNull(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + scheduleId));
        validateScheduleBelongsToReminder(schedule, reminderId);
        validateOwner(schedule.getReminder(), requesterUserId);
        return toDto(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReminderScheduleResponseDto> getByReminderId(Long reminderId, Long requesterUserId) {
        getAccessibleReminder(reminderId, requesterUserId);
        List<ReminderSchedule> schedules = reminderScheduleRepository.findByReminderIdAndDeletedAtIsNull(reminderId);

        return schedules.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReminderScheduleResponseDto> getByReminderId(Long reminderId, Long requesterUserId, Pageable pageable) {
        getAccessibleReminder(reminderId, requesterUserId);
        return reminderScheduleRepository.findByReminderIdAndDeletedAtIsNull(reminderId, pageable).map(this::toDto);
    }

    @Override
    @Transactional
    public void delete(Long reminderId, Long scheduleId, Long requesterUserId) {
        ReminderSchedule schedule = reminderScheduleRepository.findByIdAndDeletedAtIsNull(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + scheduleId));
        validateScheduleBelongsToReminder(schedule, reminderId);
        validateOwner(schedule.getReminder(), requesterUserId);
        schedule.setDeletedAt(LocalDateTime.now());
        reminderScheduleRepository.save(schedule);
        reminderInstanceService.softDeleteFutureInstancesForSchedule(schedule.getId());
    }

    private void validateOwner(Reminder reminder, Long requesterUserId) {
        if (!reminder.getUser().getId().equals(requesterUserId)) {
            throw new ForbiddenException("No permission to access this reminder schedule");
        }
    }

    private Reminder getAccessibleReminder(Long reminderId, Long requesterUserId) {
        Reminder reminder = reminderRepository.findByIdAndDeletedAtIsNull(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found: " + reminderId));
        validateOwner(reminder, requesterUserId);
        return reminder;
    }

    private void validateScheduleBelongsToReminder(ReminderSchedule schedule, Long reminderId) {
        if (!schedule.getReminder().getId().equals(reminderId)) {
            throw new ResourceNotFoundException("Schedule not found: " + schedule.getId());
        }
    }

    private ReminderScheduleResponseDto toDto(ReminderSchedule schedule) {
        return new ReminderScheduleResponseDto(
                schedule.getId(),
                schedule.getReminder().getId(),
                schedule.getType(),
                schedule.getIntervalValue(),
                schedule.getDaysOfWeek(),
                schedule.getStartDatetime(),
                schedule.getEndDatetime()
        );
    }
}
