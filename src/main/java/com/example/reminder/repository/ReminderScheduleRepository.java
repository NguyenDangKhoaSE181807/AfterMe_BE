package com.example.reminder.repository;

import com.example.reminder.entity.ReminderSchedule;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderScheduleRepository extends JpaRepository<ReminderSchedule, Long> {

    List<ReminderSchedule> findByReminderIdAndDeletedAtIsNull(Long reminderId);

    Optional<ReminderSchedule> findByIdAndDeletedAtIsNull(Long id);
}





