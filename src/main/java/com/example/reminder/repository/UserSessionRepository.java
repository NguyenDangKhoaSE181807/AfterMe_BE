package com.example.reminder.repository;

import com.example.reminder.domain.enums.SessionStatus;
import com.example.reminder.entity.User;
import com.example.reminder.entity.UserSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findFirstByUserAndDeviceIdAndStatusAndExpiresAtAfter(
            User user,
            String deviceId,
            SessionStatus status,
            LocalDateTime now
    );

    List<UserSession> findByUserAndStatusAndExpiresAtAfterOrderByLastUsedAtDesc(
            User user,
            SessionStatus status,
            LocalDateTime now
    );

    @Query("""
            select count(distinct s.user.id)
            from UserSession s
            where s.status = :status
              and s.lastUsedAt >= :from
              and s.expiresAt > :now
            """)
    long countDistinctUsersByStatusAndLastUsedAtAfter(
            @Param("status") SessionStatus status,
            @Param("from") LocalDateTime from,
            @Param("now") LocalDateTime now
    );
}
