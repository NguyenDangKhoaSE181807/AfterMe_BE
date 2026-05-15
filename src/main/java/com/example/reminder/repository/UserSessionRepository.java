package com.example.reminder.repository;

import com.example.reminder.domain.enums.SessionStatus;
import com.example.reminder.entity.User;
import com.example.reminder.entity.UserSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
