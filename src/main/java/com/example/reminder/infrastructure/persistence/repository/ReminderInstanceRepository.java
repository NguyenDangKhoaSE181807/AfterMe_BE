package com.example.reminder.infrastructure.persistence.repository;

import com.example.reminder.infrastructure.persistence.entity.ReminderInstance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderInstanceRepository extends JpaRepository<ReminderInstance, Long> {

    List<ReminderInstance> findByReminderIdAndDeletedAtIsNull(Long reminderId);
}





