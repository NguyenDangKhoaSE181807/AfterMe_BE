package com.example.reminder.infrastructure.persistence.repository;

import com.example.reminder.infrastructure.persistence.entity.Reminder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByUserIdAndDeletedAtIsNull(Long userId);

    List<Reminder> findAllByDeletedAtIsNull();

    Optional<Reminder> findByIdAndDeletedAtIsNull(Long id);
}





