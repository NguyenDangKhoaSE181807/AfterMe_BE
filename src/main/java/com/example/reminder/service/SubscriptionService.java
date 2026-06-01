package com.example.reminder.service;

import com.example.reminder.dto.subscription.AddFamilyMemberRequest;
import com.example.reminder.dto.subscription.PurchaseSePayResponse;
import com.example.reminder.dto.subscription.FamilyMemberResponseDto;
import com.example.reminder.dto.subscription.PurchaseSubscriptionRequest;
import com.example.reminder.dto.subscription.PurchaseVnPayResponse;
import com.example.reminder.dto.subscription.SubscriptionResponseDto;
import com.example.reminder.dto.subscription.UserSubscriptionDto;
import com.example.reminder.entity.UserSubscription;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

public interface SubscriptionService {

    Optional<UserSubscriptionDto> findById(Long id);

    UserSubscription save(UserSubscription subscription);

    SubscriptionResponseDto purchaseSubscription(Authentication authentication, PurchaseSubscriptionRequest request);

        PurchaseVnPayResponse initiateVnPayPurchase(Authentication authentication, PurchaseSubscriptionRequest request,
            HttpServletRequest httpRequest);

    PurchaseVnPayResponse confirmVnPayPayment(Map<String, String> params);

        PurchaseSePayResponse initiateSePayPurchase(Authentication authentication,
            PurchaseSubscriptionRequest request);

        void handleSePayWebhook(String payload, String signature, String timestamp);

    FamilyMemberResponseDto addFamilyMember(Authentication authentication, AddFamilyMemberRequest request);

    SubscriptionResponseDto getCurrentSubscription(Authentication authentication);

    void cancelCurrentSubscription(Authentication authentication);

    List<SubscriptionResponseDto> getUserSubscriptionHistory(Authentication authentication);

    /**
     * Get all subscriptions (admin only)
     */
    List<SubscriptionResponseDto> getAllSubscriptions(Authentication authentication);

    /**
     * Check if user has an active subscription
     */
    boolean hasActiveSubscription(Long userId);
}
