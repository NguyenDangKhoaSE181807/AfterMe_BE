package com.example.reminder.service.impl;

import com.example.reminder.entity.UserSubscription;
import com.example.reminder.repository.UserSubscriptionRepository;
import com.example.reminder.service.SubscriptionService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final UserSubscriptionRepository userSubscriptionRepository;

    @Override
    public List<UserSubscription> findByUserId(Long userId) {
        return userSubscriptionRepository.findByUserIdAndDeletedAtIsNull(userId);
    }

    @Override
    public Optional<UserSubscription> findById(Long id) {
        return userSubscriptionRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public UserSubscription save(UserSubscription subscription) {
        return userSubscriptionRepository.save(subscription);
    }
}
