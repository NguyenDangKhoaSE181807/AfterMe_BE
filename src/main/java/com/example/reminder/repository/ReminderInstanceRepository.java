package com.example.reminder.repository;

import com.example.reminder.entity.ReminderInstance;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReminderInstanceRepository extends JpaRepository<ReminderInstance, Long> {

    List<ReminderInstance> findByReminderIdAndDeletedAtIsNull(Long reminderId);

    Page<ReminderInstance> findByReminderIdAndDeletedAtIsNull(Long reminderId, Pageable pageable);

    Optional<ReminderInstance> findByIdAndReminderIdAndDeletedAtIsNull(Long id, Long reminderId);

    List<ReminderInstance> findByReminderIdAndScheduleIdAndDeletedAtIsNullAndScheduledTimeBetweenOrderByScheduledTimeAsc(
        Long reminderId,
        Long scheduleId,
        LocalDateTime start,
        LocalDateTime end
    );

    @Modifying
    @Query("""
        update ReminderInstance reminderInstance
           set reminderInstance.deletedAt = :deletedAt
         where reminderInstance.reminder.id = :reminderId
           and reminderInstance.deletedAt is null
           and reminderInstance.scheduledTime >= :fromTime
        """)
    int softDeleteFutureInstancesByReminderId(
        @Param("reminderId") Long reminderId,
        @Param("fromTime") LocalDateTime fromTime,
        @Param("deletedAt") LocalDateTime deletedAt
    );

    @Modifying
    @Query("""
        update ReminderInstance reminderInstance
           set reminderInstance.deletedAt = :deletedAt
         where reminderInstance.reminder.id = :reminderId
           and reminderInstance.schedule.id = :scheduleId
           and reminderInstance.deletedAt is null
           and reminderInstance.scheduledTime >= :fromTime
        """)
    int softDeleteFutureInstancesByReminderIdAndScheduleId(
        @Param("reminderId") Long reminderId,
        @Param("scheduleId") Long scheduleId,
        @Param("fromTime") LocalDateTime fromTime,
        @Param("deletedAt") LocalDateTime deletedAt
    );
}
