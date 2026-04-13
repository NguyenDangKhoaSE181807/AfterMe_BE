package com.example.reminder.repository;

import com.example.reminder.entity.EscalationLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EscalationLogRepository extends JpaRepository<EscalationLog, Long> {

    List<EscalationLog> findByReminderInstanceIdAndDeletedAtIsNull(Long reminderInstanceId);
}





