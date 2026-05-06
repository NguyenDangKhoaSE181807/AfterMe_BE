package com.example.reminder.service;

import com.example.reminder.dto.reminderinstance.ReminderInstanceResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReminderInstanceService {

    Page<ReminderInstanceResponseDto> getByReminderId(Long reminderId, Long requesterUserId, Pageable pageable);

    ReminderInstanceResponseDto getById(Long reminderId, Long instanceId, Long requesterUserId);

    void syncRollingWindowForSchedule(Long scheduleId);

    void softDeleteFutureInstancesForSchedule(Long scheduleId);

    void softDeleteFutureInstancesForReminder(Long reminderId);

    void syncRollingWindowsForReminder(Long reminderId);

    void refreshRollingWindowsForActiveReminders();
}