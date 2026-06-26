package com.example.reminder.repository;

import com.example.reminder.entity.UserActivityState;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActivityStateRepository extends JpaRepository<UserActivityState, Long> {

    Optional<UserActivityState> findByUserIdAndDeviceId(Long userId, String deviceId);

    List<UserActivityState> findByUserId(Long userId);
}
