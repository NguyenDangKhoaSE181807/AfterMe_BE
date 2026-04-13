package com.example.reminder.repository;

import com.example.reminder.entity.SubscriptionHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionHistoryRepository extends JpaRepository<SubscriptionHistory, Long> {

    List<SubscriptionHistory> findByUserIdOrderByChangedAtDesc(Long userId);
}
