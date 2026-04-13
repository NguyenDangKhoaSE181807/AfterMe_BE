package com.example.reminder.service;

import com.example.reminder.dto.habit.CreateHabitCommand;
import com.example.reminder.dto.habit.UpdateHabitCommand;
import com.example.reminder.domain.model.HabitModel;
import java.util.List;

public interface HabitService {

    List<HabitModel> findAll(Long userId);

    HabitModel findById(Long id);

    HabitModel create(CreateHabitCommand command);

    HabitModel update(Long id, UpdateHabitCommand command);

    void delete(Long id);

    HabitModel getActiveHabit(Long id);
}
