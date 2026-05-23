package com.example.reminder.service.impl;

import com.example.reminder.domain.enums.ReminderStatus;
import com.example.reminder.domain.enums.ReminderSourceType;
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
import com.example.reminder.service.ReminderInstanceService;
import com.example.reminder.service.ReminderService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderServiceImpl implements ReminderService {

    private final ReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final HabitRepository habitRepository;
    private final ReminderInstanceService reminderInstanceService;

    @Override
    @Transactional(readOnly = true)
    public List<ReminderModel> findAll(Long userId) {
        return (userId == null
                ? reminderRepository.findAllByDeletedAtIsNull()
                : reminderRepository.findByUserIdAndSourceTypeAndDeletedAtIsNull(userId, ReminderSourceType.USER))
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReminderModel> findAll(Long userId, Pageable pageable) {
        Page<Reminder> page = userId == null
                ? reminderRepository.findAllByDeletedAtIsNull(pageable)
                : reminderRepository.findByUserIdAndSourceTypeAndDeletedAtIsNull(userId, ReminderSourceType.USER, pageable);

        return page.map(this::toModel);
    }

    @Override
    @Transactional(readOnly = true)
    public ReminderModel findById(Long id) {
        return toModel(getActiveReminderEntity(id));
    }

    @Override
    @Transactional
    public ReminderModel create(CreateReminderCommand command) {
        User user = getActiveUserEntity(command.userId());
        Habit habit = command.habitId() == null ? null : getActiveHabitEntity(command.habitId());

        Reminder reminder = new Reminder();
        reminder.setUser(user);
        reminder.setHabit(habit);
        reminder.setTitle(command.title());
        reminder.setDescription(command.description());
        reminder.setTone(command.tone());
        reminder.setSafetyEnabled(command.safetyEnabled() != null ? command.safetyEnabled() : false);
        reminder.setStatus(ReminderStatus.ACTIVE); // Mặc định ACTIVE
        reminder.setSourceType(ReminderSourceType.USER); // Default to USER for reminders created via API
        reminder.setCreatedAt(LocalDateTime.now());

        return toModel(reminderRepository.save(reminder));
    }

    @Override
    @Transactional
    public ReminderModel update(Long id, UpdateReminderCommand command) {
        Reminder reminder = getActiveReminderEntity(id);
        User user = getActiveUserEntity(command.userId());
        Habit habit = command.habitId() == null ? null : getActiveHabitEntity(command.habitId());

        // Kiểm tra user có quyền update reminder này không (reminder phải của user đó)
        if (!reminder.getUser().getId().equals(command.userId())) {
            throw new IllegalStateException("You don't have permission to update this reminder");
        }

        reminder.setUser(user);
        reminder.setHabit(habit);
        reminder.setTitle(command.title());
        reminder.setDescription(command.description());
        reminder.setTone(command.tone());
        reminder.setSafetyEnabled(command.safetyEnabled());
        reminder.setUpdatedAt(LocalDateTime.now());

        return toModel(reminderRepository.save(reminder));
    }

    @Override
    @Transactional
    public ReminderModel pause(Long id) {
        Reminder reminder = getActiveReminderEntity(id);
        reminder.setStatus(ReminderStatus.PAUSED);
        reminder.setUpdatedAt(LocalDateTime.now());
        Reminder saved = reminderRepository.save(reminder);
        reminderInstanceService.softDeleteFutureInstancesForReminder(saved.getId());
        return toModel(saved);
    }

    @Override
    @Transactional
    public ReminderModel resume(Long id) {
        Reminder reminder = getActiveReminderEntity(id);
        if (reminder.getStatus() != ReminderStatus.PAUSED) {
            throw new IllegalStateException("Reminder must be PAUSED to resume");
        }
        reminder.setStatus(ReminderStatus.ACTIVE);
        reminder.setUpdatedAt(LocalDateTime.now());
        Reminder saved = reminderRepository.save(reminder);
        reminderInstanceService.syncRollingWindowsForReminder(saved.getId());
        return toModel(saved);
    }

    @Override
    @Transactional
    public void archive(Long id) {
        Reminder reminder = getActiveReminderEntity(id);
        reminder.setStatus(ReminderStatus.ARCHIVED);
        reminder.setUpdatedAt(LocalDateTime.now());
        // Không thay đổi deletedAt - chỉ thay đổi status
        Reminder saved = reminderRepository.save(reminder);
        reminderInstanceService.softDeleteFutureInstancesForReminder(saved.getId());
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
        Long habitId = null;
        try {
            habitId = reminder.getHabit() == null ? null : reminder.getHabit().getId();
        } catch (RuntimeException ex) {
            log.warn("Unable to resolve habit for reminder {}. Returning reminder without habitId.", reminder.getId(), ex);
        }

        return new ReminderModel(
                reminder.getId(),
                reminder.getUser().getId(),
                habitId,
                reminder.getTitle(),
                reminder.getDescription(),
                reminder.getTone(),
                reminder.getSafetyEnabled(),
                reminder.getStatus(),
                reminder.getCreatedAt(),
                reminder.getUpdatedAt(),
                reminder.getDeletedAt()
        );
    }
}
