package com.example.reminder.application.port.out;

import com.example.reminder.domain.model.HabitModel;
import java.util.List;
import java.util.Optional;

public interface HabitPersistencePort {

    List<HabitModel> findAllActive();

    List<HabitModel> findAllActiveByUserId(Long userId);

    Optional<HabitModel> findActiveById(Long id);

    HabitModel save(HabitModel habit);
}




