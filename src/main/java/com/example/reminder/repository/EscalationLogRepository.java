package com.example.reminder.repository;

import com.example.reminder.entity.EscalationLog;
import com.example.reminder.domain.enums.NotificationType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EscalationLogRepository extends JpaRepository<EscalationLog, Long> {

    List<EscalationLog> findByReminderInstanceIdAndDeletedAtIsNull(Long reminderInstanceId);

    boolean existsByReminderInstanceIdAndLevelAndDeletedAtIsNull(Long reminderInstanceId, Integer level);

    boolean existsByReminderInstanceIdAndNotificationTypeAndDeletedAtIsNull(Long reminderInstanceId, NotificationType notificationType);
}





