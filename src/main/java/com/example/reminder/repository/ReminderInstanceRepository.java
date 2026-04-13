package com.example.reminder.repository;

import com.example.reminder.entity.ReminderInstance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderInstanceRepository extends JpaRepository<ReminderInstance, Long> {

    List<ReminderInstance> findByReminderIdAndDeletedAtIsNull(Long reminderId);
}





