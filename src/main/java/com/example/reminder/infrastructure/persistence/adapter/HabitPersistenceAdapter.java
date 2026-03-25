package com.example.reminder.infrastructure.persistence.adapter;

import com.example.reminder.application.port.out.HabitPersistencePort;
import com.example.reminder.infrastructure.persistence.entity.Habit;
import com.example.reminder.infrastructure.persistence.entity.User;
import com.example.reminder.domain.model.HabitModel;
import com.example.reminder.application.exception.ResourceNotFoundException;
import com.example.reminder.infrastructure.persistence.repository.HabitRepository;
import com.example.reminder.infrastructure.persistence.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HabitPersistenceAdapter implements HabitPersistencePort {

    private final HabitRepository habitRepository;
    private final UserRepository userRepository;

    @Override
    public List<HabitModel> findAllActive() {
        return habitRepository.findAllByDeletedAtIsNull().stream().map(this::toModel).toList();
    }

    @Override
    public List<HabitModel> findAllActiveByUserId(Long userId) {
        return habitRepository.findByUserIdAndDeletedAtIsNull(userId).stream().map(this::toModel).toList();
    }

    @Override
    public Optional<HabitModel> findActiveById(Long id) {
        return habitRepository.findByIdAndDeletedAtIsNull(id).map(this::toModel);
    }

    @Override
    public HabitModel save(HabitModel habitModel) {
        Habit habit = habitModel.id() == null
                ? new Habit()
                : habitRepository.findById(habitModel.id())
                        .orElseThrow(() -> new ResourceNotFoundException("Habit not found: " + habitModel.id()));

        User user = userRepository.findByIdAndDeletedAtIsNull(habitModel.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + habitModel.userId()));

        habit.setUser(user);
        habit.setName(habitModel.name());
        habit.setCategory(habitModel.category());
        habit.setCreatedAt(habitModel.createdAt());
        habit.setDeletedAt(habitModel.deletedAt());

        return toModel(habitRepository.save(habit));
    }

    private HabitModel toModel(Habit habit) {
        return new HabitModel(
                habit.getId(),
                habit.getUser().getId(),
                habit.getName(),
                habit.getCategory(),
                habit.getCreatedAt(),
                habit.getDeletedAt()
        );
    }
}




