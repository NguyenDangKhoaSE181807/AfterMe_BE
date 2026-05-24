package com.example.reminder.service;

import com.example.reminder.dto.reminderinstance.ReminderInstanceResponseDto;
import com.example.reminder.dto.reminderinstance.TodayReminderScheduleDto;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.reminder.domain.enums.UserResponseAction;

public interface ReminderInstanceService {

    Page<ReminderInstanceResponseDto> getByReminderId(Long reminderId, Long requesterUserId, Pageable pageable);

    ReminderInstanceResponseDto getById(Long reminderId, Long instanceId, Long requesterUserId);

    List<TodayReminderScheduleDto> getTodaySchedules(Long requesterUserId);

    void syncRollingWindowForSchedule(Long scheduleId);

    void softDeleteFutureInstancesForSchedule(Long scheduleId);

    void softDeleteFutureInstancesForReminder(Long reminderId);

    void syncRollingWindowsForReminder(Long reminderId);

    void refreshRollingWindowsForActiveReminders();

    /**
     * Handle an explicit user response for a reminder instance.
     */
    void handleUserResponse(Long userId, Long instanceId, UserResponseAction action);
}