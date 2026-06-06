package com.example.reminder.repository;

import com.example.reminder.entity.ActivityLog;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    Page<ActivityLog> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    Page<ActivityLog> findByUserIdAndDeletedAtIsNullAndCreatedAtGreaterThanEqual(
            Long userId,
            LocalDateTime createdAt,
            Pageable pageable
    );
}
