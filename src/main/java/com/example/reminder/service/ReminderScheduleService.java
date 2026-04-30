package com.example.reminder.service;

import com.example.reminder.dto.reminder.CreateReminderScheduleRequest;
import com.example.reminder.dto.reminder.ReminderScheduleResponseDto;
import com.example.reminder.dto.reminder.UpdateReminderScheduleRequest;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReminderScheduleService {

    ReminderScheduleResponseDto create(Long reminderId, Long requesterUserId, CreateReminderScheduleRequest request);

    ReminderScheduleResponseDto update(Long reminderId, Long scheduleId, Long requesterUserId, UpdateReminderScheduleRequest request);

    ReminderScheduleResponseDto getById(Long reminderId, Long scheduleId, Long requesterUserId);

    List<ReminderScheduleResponseDto> getByReminderId(Long reminderId, Long requesterUserId);

    Page<ReminderScheduleResponseDto> getByReminderId(Long reminderId, Long requesterUserId, Pageable pageable);

    void delete(Long reminderId, Long scheduleId, Long requesterUserId);
}
