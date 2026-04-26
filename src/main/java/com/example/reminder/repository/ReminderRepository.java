package com.example.reminder.repository;

import com.example.reminder.entity.Reminder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByUserIdAndDeletedAtIsNull(Long userId);

    Page<Reminder> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    List<Reminder> findAllByDeletedAtIsNull();

    Page<Reminder> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<Reminder> findByIdAndDeletedAtIsNull(Long id);
}





