package com.example.reminder.application.usecase;

import com.example.reminder.application.dto.reminder.CreateReminderCommand;
import com.example.reminder.application.dto.reminder.UpdateReminderCommand;
import com.example.reminder.application.port.out.ReminderPersistencePort;
import com.example.reminder.domain.model.HabitModel;
import com.example.reminder.domain.model.ReminderModel;
import com.example.reminder.domain.model.UserModel;
import com.example.reminder.application.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
public class ReminderCrudUseCase {

    private final ReminderPersistencePort reminderPersistencePort;
    private final UserCrudUseCase userCrudUseCase;
    private final HabitCrudUseCase habitCrudUseCase;

    public ReminderCrudUseCase(
            ReminderPersistencePort reminderPersistencePort,
            UserCrudUseCase userCrudUseCase,
            HabitCrudUseCase habitCrudUseCase
    ) {
        this.reminderPersistencePort = reminderPersistencePort;
        this.userCrudUseCase = userCrudUseCase;
        this.habitCrudUseCase = habitCrudUseCase;
    }

    public List<ReminderModel> findAll(Long userId) {
        return userId == null
                ? reminderPersistencePort.findAllActive()
                : reminderPersistencePort.findAllActiveByUserId(userId);
    }

    public ReminderModel findById(Long id) {
        return getActiveReminder(id);
    }

    public ReminderModel create(CreateReminderCommand command) {
        UserModel user = userCrudUseCase.getActiveUser(command.userId());
        HabitModel habit = command.habitId() == null ? null : habitCrudUseCase.getActiveHabit(command.habitId());

        ReminderModel reminder = new ReminderModel(
                null,
                user.id(),
                habit == null ? null : habit.id(),
                command.title(),
                command.description(),
                command.tone(),
                command.safetyEnabled(),
                command.status(),
                LocalDateTime.now(),
                null
        );

        return reminderPersistencePort.save(reminder);
    }

    public ReminderModel update(Long id, UpdateReminderCommand command) {
        ReminderModel current = getActiveReminder(id);
        UserModel user = userCrudUseCase.getActiveUser(command.userId());
        HabitModel habit = command.habitId() == null ? null : habitCrudUseCase.getActiveHabit(command.habitId());

        ReminderModel updated = new ReminderModel(
                current.id(),
                user.id(),
                habit == null ? null : habit.id(),
                command.title(),
                command.description(),
                command.tone(),
                command.safetyEnabled(),
                command.status(),
                current.createdAt(),
                null
        );

        return reminderPersistencePort.save(updated);
    }

    public void delete(Long id) {
        ReminderModel current = getActiveReminder(id);

        ReminderModel deleted = new ReminderModel(
                current.id(),
                current.userId(),
                current.habitId(),
                current.title(),
                current.description(),
                current.tone(),
                current.safetyEnabled(),
                current.status(),
                current.createdAt(),
                LocalDateTime.now()
        );

        reminderPersistencePort.save(deleted);
    }

    private ReminderModel getActiveReminder(Long id) {
        return reminderPersistencePort.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found: " + id));
    }
}




