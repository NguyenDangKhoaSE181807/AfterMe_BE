package com.example.reminder.repository;

import com.example.reminder.entity.EmailVerificationCode;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {

    Optional<EmailVerificationCode> findByUserIdAndCodeAndIsUsedFalseAndExpiresAtAfter(
            Long userId,
            String code,
            LocalDateTime now
    );

    Optional<EmailVerificationCode> findByCodeAndIsUsedFalseAndExpiresAtAfter(
            String code,
            LocalDateTime now
    );

    Optional<EmailVerificationCode> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
}
