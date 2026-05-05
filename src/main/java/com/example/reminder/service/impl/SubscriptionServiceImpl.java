package com.example.reminder.service.impl;

import com.example.reminder.dto.subscription.PurchaseSubscriptionRequest;
import com.example.reminder.dto.subscription.SubscriptionResponseDto;
import com.example.reminder.dto.subscription.UserSubscriptionDto;
import com.example.reminder.dto.plan.PlanResponseDto;
import com.example.reminder.entity.Plan;
import com.example.reminder.entity.SubscriptionHistory;
import com.example.reminder.entity.User;
import com.example.reminder.entity.UserSubscription;
import com.example.reminder.repository.UserSubscriptionRepository;
import com.example.reminder.repository.SubscriptionHistoryRepository;
import com.example.reminder.repository.PlanRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.SubscriptionService;
import com.example.reminder.service.PlanService;
import com.example.reminder.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final PlanService planService;

    @Override
    public Optional<UserSubscriptionDto> findById(Long id) {
        return userSubscriptionRepository.findByIdAndDeletedAtIsNull(id)
                .map(this::mapToUserSubscriptionDto);
    }

    @Override
    public UserSubscription save(UserSubscription subscription) {
        return userSubscriptionRepository.save(subscription);
    }

    @Override
    @Transactional
    public SubscriptionResponseDto purchaseSubscription(Authentication authentication, PurchaseSubscriptionRequest request) {
        User user = getCurrentUser(authentication);

        // Fetch plan
        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endAt = calculateEndDate(now, plan.getBillingCycle());

        // Check if user has active subscription
        Optional<UserSubscription> existingSubscription = userSubscriptionRepository.findActiveSubscriptionByUserId(user.getId());

        // If exists, create history entry and mark old one as ended
        if (existingSubscription.isPresent()) {
            UserSubscription oldSubscription = existingSubscription.get();

            // Create history record
            SubscriptionHistory history = SubscriptionHistory.builder()
                    .user(user)
                    .fromPlan(oldSubscription.getPlan())
                    .toPlan(plan)
                    .changedAt(now)
                    .build();
            subscriptionHistoryRepository.save(history);

            // Soft delete old subscription
            oldSubscription.setDeletedAt(now);
            userSubscriptionRepository.save(oldSubscription);
        }

        // Create new subscription
        UserSubscription newSubscription = UserSubscription.builder()
                .user(user)
                .plan(plan)
                .status("ACTIVE")
                .startAt(now)
                .endAt(endAt)
                .autoRenew(request.getAutoRenew() != null ? request.getAutoRenew() : true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        UserSubscription savedSubscription = userSubscriptionRepository.save(newSubscription);

        return mapToResponseDto(savedSubscription);
    }

    @Override
    public SubscriptionResponseDto getCurrentSubscription(Authentication authentication) {
        User user = getCurrentUser(authentication);
        Optional<UserSubscription> subscription = userSubscriptionRepository.findActiveSubscriptionByUserId(user.getId());

        if (subscription.isEmpty()) {
            throw new ResourceNotFoundException("No active subscription found for user");
        }

        return mapToResponseDto(subscription.get());
    }

    @Override
    public List<SubscriptionResponseDto> getUserSubscriptionHistory(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<UserSubscription> subscriptions = userSubscriptionRepository.findAllSubscriptionsByUserId(user.getId());
        return subscriptions.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private LocalDateTime calculateEndDate(LocalDateTime startDate, String billingCycle) {
        return switch (billingCycle.toUpperCase()) {
            case "MONTHLY" -> startDate.plusMonths(1);
            case "QUARTERLY" -> startDate.plusMonths(3);
            case "YEARLY", "ANNUAL" -> startDate.plusYears(1);
            case "LIFETIME" -> startDate.plusYears(100); // Effectively lifetime
            default -> startDate.plusMonths(1); // Default to monthly
        };
    }

    private SubscriptionResponseDto mapToResponseDto(UserSubscription subscription) {
        PlanResponseDto planDto = planService.findById(subscription.getPlan().getId());

        return SubscriptionResponseDto.builder()
                .id(subscription.getId())
                .userId(subscription.getUser().getId())
                .plan(planDto)
                .status(subscription.getStatus())
                .startAt(subscription.getStartAt())
                .endAt(subscription.getEndAt())
                .autoRenew(subscription.getAutoRenew())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }

    private UserSubscriptionDto mapToUserSubscriptionDto(UserSubscription subscription) {
        PlanResponseDto planDto = planService.findById(subscription.getPlan().getId());

        return UserSubscriptionDto.builder()
                .id(subscription.getId())
                .userId(subscription.getUser().getId())
                .plan(planDto)
                .status(subscription.getStatus())
                .startAt(subscription.getStartAt())
                .endAt(subscription.getEndAt())
                .autoRenew(subscription.getAutoRenew())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }

    public User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("User must be authenticated");
        }

        String email = authentication.getName();
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

   
    
}
