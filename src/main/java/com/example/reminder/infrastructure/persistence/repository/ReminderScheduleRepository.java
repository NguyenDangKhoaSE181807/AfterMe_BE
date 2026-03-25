package com.example.reminder.infrastructure.persistence.repository;

import com.example.reminder.infrastructure.persistence.entity.ReminderSchedule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderScheduleRepository extends JpaRepository<ReminderSchedule, Long> {

    List<ReminderSchedule> findByReminderIdAndDeletedAtIsNull(Long reminderId);
}





