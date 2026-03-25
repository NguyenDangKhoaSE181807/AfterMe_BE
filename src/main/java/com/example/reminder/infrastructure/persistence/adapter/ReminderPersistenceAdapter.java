package com.example.reminder.infrastructure.persistence.adapter;

import com.example.reminder.application.port.out.ReminderPersistencePort;
import com.example.reminder.infrastructure.persistence.entity.Habit;
import com.example.reminder.infrastructure.persistence.entity.Reminder;
import com.example.reminder.infrastructure.persistence.entity.User;
import com.example.reminder.domain.model.ReminderModel;
import com.example.reminder.application.exception.ResourceNotFoundException;
import com.example.reminder.infrastructure.persistence.repository.HabitRepository;
import com.example.reminder.infrastructure.persistence.repository.ReminderRepository;
import com.example.reminder.infrastructure.persistence.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReminderPersistenceAdapter implements ReminderPersistencePort {

    private final ReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final HabitRepository habitRepository;

    @Override
    public List<ReminderModel> findAllActive() {
        return reminderRepository.findAllByDeletedAtIsNull().stream().map(this::toModel).toList();
    }

    @Override
    public List<ReminderModel> findAllActiveByUserId(Long userId) {
        return reminderRepository.findByUserIdAndDeletedAtIsNull(userId).stream().map(this::toModel).toList();
    }

    @Override
    public Optional<ReminderModel> findActiveById(Long id) {
        return reminderRepository.findByIdAndDeletedAtIsNull(id).map(this::toModel);
    }

    @Override
    public ReminderModel save(ReminderModel reminderModel) {
        Reminder reminder = reminderModel.id() == null
                ? new Reminder()
                : reminderRepository.findById(reminderModel.id())
                        .orElseThrow(() -> new ResourceNotFoundException("Reminder not found: " + reminderModel.id()));

        User user = userRepository.findByIdAndDeletedAtIsNull(reminderModel.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + reminderModel.userId()));

        Habit habit = null;
        if (reminderModel.habitId() != null) {
            habit = habitRepository.findByIdAndDeletedAtIsNull(reminderModel.habitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Habit not found: " + reminderModel.habitId()));
        }

        reminder.setUser(user);
        reminder.setHabit(habit);
        reminder.setTitle(reminderModel.title());
        reminder.setDescription(reminderModel.description());
        reminder.setTone(reminderModel.tone());
        reminder.setSafetyEnabled(reminderModel.safetyEnabled());
        reminder.setStatus(reminderModel.status());
        reminder.setCreatedAt(reminderModel.createdAt());
        reminder.setDeletedAt(reminderModel.deletedAt());

        return toModel(reminderRepository.save(reminder));
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




