package com.example.reminder.service;

import com.example.reminder.entity.UserSubscription;
import java.util.List;
import java.util.Optional;

public interface SubscriptionService {

    List<UserSubscription> findByUserId(Long userId);

    Optional<UserSubscription> findById(Long id);

    UserSubscription save(UserSubscription subscription);
}
