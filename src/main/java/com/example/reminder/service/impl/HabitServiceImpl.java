package com.example.reminder.service.impl;

import com.example.reminder.dto.habit.CreateHabitCommand;
import com.example.reminder.dto.habit.UpdateHabitCommand;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.domain.model.HabitModel;
import com.example.reminder.entity.Habit;
import com.example.reminder.entity.User;
import com.example.reminder.repository.HabitRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.HabitService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HabitServiceImpl implements HabitService {

    private final HabitRepository habitRepository;
    private final UserRepository userRepository;

    @Override
    public List<HabitModel> findAll(Long userId) {
        return (userId == null
                ? habitRepository.findAllByDeletedAtIsNull()
                : habitRepository.findByUserIdAndDeletedAtIsNull(userId))
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public HabitModel findById(Long id) {
        return getActiveHabit(id);
    }

    @Override
    public HabitModel create(CreateHabitCommand command) {
        User user = getActiveUserEntity(command.userId());

        Habit habit = new Habit();
        habit.setUser(user);
        habit.setName(command.name());
        habit.setCategory(command.category());
        habit.setCreatedAt(LocalDateTime.now());

        return toModel(habitRepository.save(habit));
    }

    @Override
    public HabitModel update(Long id, UpdateHabitCommand command) {
        Habit habit = getActiveHabitEntity(id);
        User user = getActiveUserEntity(command.userId());

        habit.setUser(user);
        habit.setName(command.name());
        habit.setCategory(command.category());
        habit.setDeletedAt(null);

        return toModel(habitRepository.save(habit));
    }

    @Override
    public void delete(Long id) {
        Habit habit = getActiveHabitEntity(id);
        habit.setDeletedAt(LocalDateTime.now());
        habitRepository.save(habit);
    }

    @Override
    public HabitModel getActiveHabit(Long id) {
        return toModel(getActiveHabitEntity(id));
    }

    private Habit getActiveHabitEntity(Long id) {
        return habitRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found: " + id));
    }

    private User getActiveUserEntity(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
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
