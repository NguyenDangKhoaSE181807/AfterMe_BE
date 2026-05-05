package com.example.reminder.service;

import com.example.reminder.dto.subscription.PurchaseSubscriptionRequest;
import com.example.reminder.dto.subscription.SubscriptionResponseDto;
import com.example.reminder.dto.subscription.UserSubscriptionDto;
import com.example.reminder.entity.UserSubscription;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;

public interface SubscriptionService {

    Optional<UserSubscriptionDto> findById(Long id);

    UserSubscription save(UserSubscription subscription);

    SubscriptionResponseDto purchaseSubscription(Authentication authentication, PurchaseSubscriptionRequest request);

    SubscriptionResponseDto getCurrentSubscription(Authentication authentication);

    List<SubscriptionResponseDto> getUserSubscriptionHistory(Authentication authentication);

}
