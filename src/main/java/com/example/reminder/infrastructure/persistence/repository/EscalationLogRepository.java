package com.example.reminder.infrastructure.persistence.repository;

import com.example.reminder.infrastructure.persistence.entity.EscalationLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EscalationLogRepository extends JpaRepository<EscalationLog, Long> {

    List<EscalationLog> findByReminderInstanceIdAndDeletedAtIsNull(Long reminderInstanceId);
}





