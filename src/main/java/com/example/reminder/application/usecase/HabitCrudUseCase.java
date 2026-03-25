package com.example.reminder.application.usecase;

import com.example.reminder.application.dto.habit.CreateHabitCommand;
import com.example.reminder.application.dto.habit.UpdateHabitCommand;
import com.example.reminder.application.port.out.HabitPersistencePort;
import com.example.reminder.domain.model.HabitModel;
import com.example.reminder.domain.model.UserModel;
import com.example.reminder.application.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
public class HabitCrudUseCase {

    private final HabitPersistencePort habitPersistencePort;
    private final UserCrudUseCase userCrudUseCase;

    public HabitCrudUseCase(HabitPersistencePort habitPersistencePort, UserCrudUseCase userCrudUseCase) {
        this.habitPersistencePort = habitPersistencePort;
        this.userCrudUseCase = userCrudUseCase;
    }

    public List<HabitModel> findAll(Long userId) {
        return userId == null
                ? habitPersistencePort.findAllActive()
                : habitPersistencePort.findAllActiveByUserId(userId);
    }

    public HabitModel findById(Long id) {
        return getActiveHabit(id);
    }

    public HabitModel create(CreateHabitCommand command) {
        UserModel user = userCrudUseCase.getActiveUser(command.userId());

        HabitModel habit = new HabitModel(
                null,
                user.id(),
                command.name(),
                command.category(),
                LocalDateTime.now(),
                null
        );

        return habitPersistencePort.save(habit);
    }

    public HabitModel update(Long id, UpdateHabitCommand command) {
        HabitModel current = getActiveHabit(id);
        UserModel user = userCrudUseCase.getActiveUser(command.userId());

        HabitModel updated = new HabitModel(
                current.id(),
                user.id(),
                command.name(),
                command.category(),
                current.createdAt(),
                null
        );

        return habitPersistencePort.save(updated);
    }

    public void delete(Long id) {
        HabitModel current = getActiveHabit(id);

        HabitModel deleted = new HabitModel(
                current.id(),
                current.userId(),
                current.name(),
                current.category(),
                current.createdAt(),
                LocalDateTime.now()
        );

        habitPersistencePort.save(deleted);
    }

    public HabitModel getActiveHabit(Long id) {
        return habitPersistencePort.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found: " + id));
    }
}




