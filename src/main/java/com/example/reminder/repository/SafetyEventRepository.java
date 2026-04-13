package com.example.reminder.repository;

import com.example.reminder.entity.SafetyEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SafetyEventRepository extends JpaRepository<SafetyEvent, Long> {

    List<SafetyEvent> findByUserIdAndDeletedAtIsNull(Long userId);
}





