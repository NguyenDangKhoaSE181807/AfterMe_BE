package com.example.reminder.repository;

import com.example.reminder.entity.UserResponse;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserResponseRepository extends JpaRepository<UserResponse, Long> {

    List<UserResponse> findByReminderInstanceIdAndDeletedAtIsNull(Long reminderInstanceId);
}





