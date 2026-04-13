package com.example.reminder.repository;

import com.example.reminder.entity.UserSubscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    List<UserSubscription> findByUserIdAndDeletedAtIsNull(Long userId);

    Optional<UserSubscription> findByIdAndDeletedAtIsNull(Long id);
}
