package com.example.reminder.repository;

import com.example.reminder.entity.ReminderInstance;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import com.example.reminder.domain.enums.ReminderStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReminderInstanceRepository extends JpaRepository<ReminderInstance, Long> {

    List<ReminderInstance> findByReminderIdAndDeletedAtIsNull(Long reminderId);

    Page<ReminderInstance> findByReminderIdAndDeletedAtIsNull(Long reminderId, Pageable pageable);

    Optional<ReminderInstance> findByIdAndReminderIdAndDeletedAtIsNull(Long id, Long reminderId);

    long countByDeletedAtIsNull();

    long countByDeletedAtIsNullAndLastNotificationAtBetween(LocalDateTime from, LocalDateTime to);

    long countByDeletedAtIsNullAndScheduledTimeBetween(LocalDateTime from, LocalDateTime to);

    long countByDeletedAtIsNullAndStatusIn(List<com.example.reminder.domain.enums.ReminderInstanceStatus> statuses);

    long countByDeletedAtIsNullAndStatusInAndScheduledTimeBetween(
        List<com.example.reminder.domain.enums.ReminderInstanceStatus> statuses,
        LocalDateTime from,
        LocalDateTime to
    );

    List<ReminderInstance> findByDeletedAtIsNullAndScheduledTimeBetweenOrderByScheduledTimeAsc(
        LocalDateTime from,
        LocalDateTime to
    );

        @Query("""
                select reminderInstance
                    from ReminderInstance reminderInstance
                    join reminderInstance.reminder reminder
                 where reminder.user.id = :userId
                     and reminder.status = :status
                     and reminder.deletedAt is null
                     and reminderInstance.deletedAt is null
                     and reminderInstance.scheduledTime >= :start
                     and reminderInstance.scheduledTime < :end
                 order by reminderInstance.scheduledTime asc
                """)
        List<ReminderInstance> findScheduledForUserBetween(
                        @Param("userId") Long userId,
                        @Param("status") ReminderStatus status,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end
        );

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
