package com.example.reminder.service;

import com.example.reminder.domain.enums.DayOfWeek;
import com.example.reminder.domain.enums.ReminderStatus;
import com.example.reminder.domain.enums.ScheduleType;
import com.example.reminder.dto.reminder.CreateReminderScheduleRequest;
import com.example.reminder.dto.reminder.ReminderScheduleResponseDto;
import com.example.reminder.dto.reminder.UpdateReminderScheduleRequest;
import com.example.reminder.exception.ForbiddenException;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.entity.Reminder;
import com.example.reminder.entity.ReminderSchedule;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.ReminderScheduleRepository;
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
public class ReminderScheduleService {

    private final ReminderScheduleRepository reminderScheduleRepository;
    private final ReminderRepository reminderRepository;

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

        // Xử lý logic tự động chuyển DAILY nếu chọn tất cả các ngày trong tuần
        if (request.daysOfWeek() != null && request.daysOfWeek().size() == 7) {
            schedule.setType(ScheduleType.DAILY);
            schedule.setDaysOfWeek(Set.of()); // Không cần lưu daysOfWeek cho DAILY
        } else {
            schedule.setType(request.type());
            if (request.daysOfWeek() != null) {
                schedule.setDaysOfWeek(request.daysOfWeek());
            }
        }

        // Thiết lập intervalValue
        if (request.intervalValue() != null) {
            schedule.setIntervalValue(request.intervalValue());
        }

        ReminderSchedule saved = reminderScheduleRepository.save(schedule);
        return toDto(saved);
    }

    @Transactional
    public ReminderScheduleResponseDto update(Long reminderId, Long scheduleId, Long requesterUserId, UpdateReminderScheduleRequest request) {
        ReminderSchedule schedule = reminderScheduleRepository.findByIdAndDeletedAtIsNull(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + scheduleId));

        validateScheduleBelongsToReminder(schedule, reminderId);
        validateOwner(schedule.getReminder(), requesterUserId);
        if (schedule.getReminder().getStatus() == ReminderStatus.ARCHIVED) {
            throw new ForbiddenException("Cannot update schedule for archived reminder");
        }

        schedule.setType(request.type());
        schedule.setStartDatetime(request.startDatetime());
        schedule.setEndDatetime(request.endDatetime());
        schedule.setUpdatedAt(LocalDateTime.now());

        // Xử lý logic tự động chuyển DAILY
        if (request.daysOfWeek() != null && request.daysOfWeek().size() == 7) {
            schedule.setType(ScheduleType.DAILY);
            schedule.setDaysOfWeek(Set.of());
        } else {
            if (request.daysOfWeek() != null) {
                schedule.setDaysOfWeek(request.daysOfWeek());
            }
        }

        if (request.intervalValue() != null) {
            schedule.setIntervalValue(request.intervalValue());
        }

        ReminderSchedule updated = reminderScheduleRepository.save(schedule);
        return toDto(updated);
    }

    @Transactional(readOnly = true)
    public ReminderScheduleResponseDto getById(Long reminderId, Long scheduleId, Long requesterUserId) {
        ReminderSchedule schedule = reminderScheduleRepository.findByIdAndDeletedAtIsNull(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + scheduleId));
        validateScheduleBelongsToReminder(schedule, reminderId);
        validateOwner(schedule.getReminder(), requesterUserId);
        return toDto(schedule);
    }

    @Transactional(readOnly = true)
    public List<ReminderScheduleResponseDto> getByReminderId(Long reminderId, Long requesterUserId) {
        getAccessibleReminder(reminderId, requesterUserId);
        List<ReminderSchedule> schedules = reminderScheduleRepository.findByReminderIdAndDeletedAtIsNull(reminderId);

        return schedules
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ReminderScheduleResponseDto> getByReminderId(Long reminderId, Long requesterUserId, Pageable pageable) {
        getAccessibleReminder(reminderId, requesterUserId);
        return reminderScheduleRepository.findByReminderIdAndDeletedAtIsNull(reminderId, pageable)
                .map(this::toDto);
    }

    @Transactional
    public void delete(Long reminderId, Long scheduleId, Long requesterUserId) {
        ReminderSchedule schedule = reminderScheduleRepository.findByIdAndDeletedAtIsNull(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + scheduleId));
        validateScheduleBelongsToReminder(schedule, reminderId);
        validateOwner(schedule.getReminder(), requesterUserId);
        schedule.setDeletedAt(LocalDateTime.now());
        reminderScheduleRepository.save(schedule);
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
