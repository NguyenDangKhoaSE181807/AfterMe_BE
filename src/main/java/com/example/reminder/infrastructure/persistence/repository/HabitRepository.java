package com.example.reminder.infrastructure.persistence.repository;

import com.example.reminder.infrastructure.persistence.entity.Habit;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitRepository extends JpaRepository<Habit, Long> {

    List<Habit> findByUserIdAndDeletedAtIsNull(Long userId);

    List<Habit> findAllByDeletedAtIsNull();

    Optional<Habit> findByIdAndDeletedAtIsNull(Long id);
}





