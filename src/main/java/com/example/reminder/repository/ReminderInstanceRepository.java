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

        @Query("""
                select ri from ReminderInstance ri
                join ri.reminder r
                where ri.deletedAt is null
                    and r.sourceType = :sourceType
                    and ri.status in ('PENDING','SNOOZED')
                    and (
                             (ri.nextRemindAt is not null and ri.nextRemindAt <= :now)
                        or (ri.nextRemindAt is null and ri.lastNotificationAt is not null and ri.lastNotificationAt <= :oneHourAgo)
                    )
        """)
        List<ReminderInstance> findDueForEscalation(@Param("now") LocalDateTime now,
                                                                                                @Param("oneHourAgo") LocalDateTime oneHourAgo,
                                                                                                @Param("sourceType") com.example.reminder.domain.enums.ReminderSourceType sourceType);

    @Query("""
                select ri from ReminderInstance ri
                join ri.reminder r
                where ri.deletedAt is null
                    and r.sourceType = :sourceType
                    and ri.status = com.example.reminder.domain.enums.ReminderInstanceStatus.PENDING
                    and ri.lastNotificationAt is null
                    and ri.scheduledTime <= :now
        """)
    List<ReminderInstance> findDueForInitialPush(@Param("now") LocalDateTime now,
                                                  @Param("sourceType") com.example.reminder.domain.enums.ReminderSourceType sourceType);

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
