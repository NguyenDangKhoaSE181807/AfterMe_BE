package com.example.reminder.repository;

import com.example.reminder.domain.enums.ReminderStatus;
import com.example.reminder.domain.enums.ReminderSourceType;
import com.example.reminder.entity.Reminder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByUserIdAndDeletedAtIsNull(Long userId);

    Page<Reminder> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    List<Reminder> findByUserIdAndSourceTypeAndDeletedAtIsNull(Long userId, ReminderSourceType sourceType);

    Page<Reminder> findByUserIdAndSourceTypeAndDeletedAtIsNull(Long userId, ReminderSourceType sourceType, Pageable pageable);

    List<Reminder> findAllByDeletedAtIsNull();

    Page<Reminder> findAllByDeletedAtIsNull(Pageable pageable);

    List<Reminder> findByStatusAndDeletedAtIsNull(ReminderStatus status);

    Optional<Reminder> findByIdAndDeletedAtIsNull(Long id);

    long countByDeletedAtIsNull();

    long countByDeletedAtIsNullAndStatus(ReminderStatus status);

    long countByDeletedAtIsNullAndCreatedAtGreaterThanEqual(LocalDateTime from);

    @Query("""
            select reminder
              from Reminder reminder
              join reminder.user user
             where reminder.deletedAt is null
               and (:qPattern is null
                    or lower(reminder.title) like :qPattern
                    or lower(user.email) like :qPattern
                    or lower(user.fullName) like :qPattern)
               and (:status is null or reminder.status = :status)
               and (:userId is null or user.id = :userId)
            """)
    Page<Reminder> searchAdminReminders(
            @Param("qPattern") String qPattern,
            @Param("status") ReminderStatus status,
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
            select reminder
              from Reminder reminder
             where reminder.deletedAt is null
               and reminder.createdAt >= :from
               and reminder.createdAt < :to
             order by reminder.createdAt asc
            """)
    List<Reminder> findCreatedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
