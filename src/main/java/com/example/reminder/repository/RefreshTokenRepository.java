package com.example.reminder.repository;

import com.example.reminder.entity.RefreshToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(String tokenHash, LocalDateTime now);

    List<RefreshToken> findBySessionIdAndRevokedAtIsNullAndExpiresAtAfter(Long sessionId, LocalDateTime now);
}
