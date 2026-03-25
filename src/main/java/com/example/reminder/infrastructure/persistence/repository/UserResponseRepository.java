package com.example.reminder.infrastructure.persistence.repository;

import com.example.reminder.infrastructure.persistence.entity.UserResponse;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserResponseRepository extends JpaRepository<UserResponse, Long> {

    List<UserResponse> findByReminderInstanceIdAndDeletedAtIsNull(Long reminderInstanceId);
}





