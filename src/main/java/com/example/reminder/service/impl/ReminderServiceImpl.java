package com.example.reminder.service.impl;

import com.example.reminder.dto.reminder.CreateReminderCommand;
import com.example.reminder.dto.reminder.UpdateReminderCommand;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.domain.model.ReminderModel;
import com.example.reminder.entity.Habit;
import com.example.reminder.entity.Reminder;
import com.example.reminder.entity.User;
import com.example.reminder.repository.HabitRepository;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.ReminderService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReminderServiceImpl implements ReminderService {

    private final ReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final HabitRepository habitRepository;

    @Override
    public List<ReminderModel> findAll(Long userId) {
        return (userId == null
                ? reminderRepository.findAllByDeletedAtIsNull()
                : reminderRepository.findByUserIdAndDeletedAtIsNull(userId))
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public ReminderModel findById(Long id) {
        return toModel(getActiveReminderEntity(id));
    }

    @Override
    public ReminderModel create(CreateReminderCommand command) {
        User user = getActiveUserEntity(command.userId());
        Habit habit = command.habitId() == null ? null : getActiveHabitEntity(command.habitId());

        Reminder reminder = new Reminder();
        reminder.setUser(user);
        reminder.setHabit(habit);
        reminder.setTitle(command.title());
        reminder.setDescription(command.description());
        reminder.setTone(command.tone());
        reminder.setSafetyEnabled(command.safetyEnabled());
        reminder.setStatus(command.status());
        reminder.setCreatedAt(LocalDateTime.now());

        return toModel(reminderRepository.save(reminder));
    }

    @Override
    public ReminderModel update(Long id, UpdateReminderCommand command) {
        Reminder reminder = getActiveReminderEntity(id);
        User user = getActiveUserEntity(command.userId());
        Habit habit = command.habitId() == null ? null : getActiveHabitEntity(command.habitId());

        reminder.setUser(user);
        reminder.setHabit(habit);
        reminder.setTitle(command.title());
        reminder.setDescription(command.description());
        reminder.setTone(command.tone());
        reminder.setSafetyEnabled(command.safetyEnabled());
        reminder.setStatus(command.status());
        reminder.setDeletedAt(null);

        return toModel(reminderRepository.save(reminder));
    }

    @Override
    public void delete(Long id) {
        Reminder reminder = getActiveReminderEntity(id);
        reminder.setDeletedAt(LocalDateTime.now());
        reminderRepository.save(reminder);
    }

    private Reminder getActiveReminderEntity(Long id) {
        return reminderRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found: " + id));
    }

    private User getActiveUserEntity(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private Habit getActiveHabitEntity(Long id) {
        return habitRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found: " + id));
    }

    private ReminderModel toModel(Reminder reminder) {
        return new ReminderModel(
                reminder.getId(),
                reminder.getUser().getId(),
                reminder.getHabit() == null ? null : reminder.getHabit().getId(),
                reminder.getTitle(),
                reminder.getDescription(),
                reminder.getTone(),
                reminder.getSafetyEnabled(),
                reminder.getStatus(),
                reminder.getCreatedAt(),
                reminder.getDeletedAt()
        );
    }
}
