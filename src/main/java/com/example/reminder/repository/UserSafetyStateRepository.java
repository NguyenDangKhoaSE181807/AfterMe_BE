package com.example.reminder.repository;

import com.example.reminder.entity.UserSafetyState;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSafetyStateRepository extends JpaRepository<UserSafetyState, Long> {
    Optional<UserSafetyState> findByUserId(Long userId);
}
