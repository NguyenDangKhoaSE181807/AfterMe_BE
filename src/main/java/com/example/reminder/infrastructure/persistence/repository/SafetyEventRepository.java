package com.example.reminder.infrastructure.persistence.repository;

import com.example.reminder.infrastructure.persistence.entity.SafetyEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SafetyEventRepository extends JpaRepository<SafetyEvent, Long> {

    List<SafetyEvent> findByUserIdAndDeletedAtIsNull(Long userId);
}





