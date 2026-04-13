package com.example.reminder.service;

import com.example.reminder.dto.reminder.CreateReminderCommand;
import com.example.reminder.dto.reminder.UpdateReminderCommand;
import com.example.reminder.domain.model.ReminderModel;
import java.util.List;

public interface ReminderService {

    List<ReminderModel> findAll(Long userId);

    ReminderModel findById(Long id);

    ReminderModel create(CreateReminderCommand command);

    ReminderModel update(Long id, UpdateReminderCommand command);

    void delete(Long id);
}
