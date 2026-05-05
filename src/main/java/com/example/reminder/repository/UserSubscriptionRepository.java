package com.example.reminder.repository;

import com.example.reminder.entity.UserSubscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    List<UserSubscription> findByUserIdAndDeletedAtIsNull(Long userId);

    Optional<UserSubscription> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT us FROM UserSubscription us WHERE us.user.id = :userId AND us.deletedAt IS NULL AND CURRENT_TIMESTAMP BETWEEN us.startAt AND us.endAt ORDER BY us.startAt DESC LIMIT 1")
    Optional<UserSubscription> findActiveSubscriptionByUserId(@Param("userId") Long userId);

    @Query("SELECT us FROM UserSubscription us WHERE us.user.id = :userId AND us.deletedAt IS NULL ORDER BY us.startAt DESC")
    List<UserSubscription> findAllSubscriptionsByUserId(@Param("userId") Long userId);
}
