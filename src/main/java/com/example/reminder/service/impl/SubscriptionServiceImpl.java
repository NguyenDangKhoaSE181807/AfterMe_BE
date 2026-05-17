package com.example.reminder.service.impl;

import com.example.reminder.dto.subscription.AddFamilyMemberRequest;
import com.example.reminder.dto.subscription.FamilyMemberResponseDto;
import com.example.reminder.dto.subscription.PurchaseSubscriptionRequest;
import com.example.reminder.dto.subscription.PurchaseVnPayResponse;
import com.example.reminder.dto.subscription.SubscriptionResponseDto;
import com.example.reminder.dto.subscription.UserSubscriptionDto;
import com.example.reminder.dto.plan.PlanResponseDto;
import com.example.reminder.entity.FamilyMember;
import com.example.reminder.entity.Plan;
import com.example.reminder.entity.SubscriptionHistory;
import com.example.reminder.entity.Transaction;
import com.example.reminder.entity.User;
import com.example.reminder.entity.UserSubscription;
import com.example.reminder.exception.BadRequestException;
import com.example.reminder.exception.ForbiddenException;
import com.example.reminder.repository.FamilyMemberRepository;
import com.example.reminder.repository.UserSubscriptionRepository;
import com.example.reminder.repository.SubscriptionHistoryRepository;
import com.example.reminder.repository.PlanRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.repository.TransactionRepository;
import com.example.reminder.service.SubscriptionService;
import com.example.reminder.service.PlanService;
import com.example.reminder.service.payment.VnPayService;
import com.example.reminder.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final PlanService planService;
    private final VnPayService vnPayService;
    private final TransactionRepository transactionRepository;

    @Override
    public Optional<UserSubscriptionDto> findById(Long id) {
        return userSubscriptionRepository.findById(id)
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

        if (!Boolean.TRUE.equals(plan.getIsActive())) {
            throw new BadRequestException("Only active plans can be purchased");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endAt = calculateEndDate(now, plan.getBillingCycle());

        // Check if user has active subscription
        Optional<UserSubscription> existingSubscription = userSubscriptionRepository
                .findFirstByUserIdAndDeletedAtIsNullAndStatusAndEndAtGreaterThanOrderByStartAtDesc(
                        user.getId(),
                        "ACTIVE",
                        now
                );

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
                .createdAt(now)
                .updatedAt(now)
                .build();

        UserSubscription savedSubscription = userSubscriptionRepository.save(newSubscription);

        user.setCurrentPlan(plan);
        user.setPlanExpiresAt(endAt);
        userRepository.save(user);

        if (isFamilyPlan(plan)) {
            FamilyMember familyMember = new FamilyMember();
            familyMember.setSubscription(savedSubscription);
            familyMember.setUser(user);
            familyMember.setRole("OWNER");
            familyMember.setCreatedAt(now);
            familyMemberRepository.save(familyMember);
        }

        return mapToResponseDto(savedSubscription);
    }

    @Override
    @Transactional
    public FamilyMemberResponseDto addFamilyMember(Authentication authentication, AddFamilyMemberRequest request) {
        User owner = getCurrentUser(authentication);
        UserSubscription activeSubscription = userSubscriptionRepository
                .findFirstByUserIdAndDeletedAtIsNullAndStatusAndEndAtGreaterThanOrderByStartAtDesc(
                        owner.getId(),
                        "ACTIVE",
                        LocalDateTime.now()
                )
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription found for user"));

        if (!isFamilyPlan(activeSubscription.getPlan())) {
            throw new BadRequestException("Only FAMILY plan can add family members");
        }

        FamilyMember ownerMember = familyMemberRepository.findBySubscriptionIdAndUserId(activeSubscription.getId(), owner.getId())
                .orElseThrow(() -> new ForbiddenException("Only family owner can add members"));

        if (!"OWNER".equalsIgnoreCase(ownerMember.getRole())) {
            throw new ForbiddenException("Only family owner can add members");
        }

        User member = userRepository.findByEmailAndDeletedAtIsNull(request.getMemberEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getMemberEmail()));

        if (member.getId().equals(owner.getId())) {
            throw new BadRequestException("Owner is already part of the family subscription");
        }

        if (familyMemberRepository.existsBySubscriptionIdAndUserId(activeSubscription.getId(), member.getId())) {
            throw new BadRequestException("User is already in this family subscription");
        }

        FamilyMember familyMember = new FamilyMember();
        familyMember.setSubscription(activeSubscription);
        familyMember.setUser(member);
        familyMember.setRole("MEMBER");
        familyMember.setCreatedAt(LocalDateTime.now());

        FamilyMember saved = familyMemberRepository.save(familyMember);
        return mapToFamilyMemberResponseDto(saved);
    }

    @Override
    public SubscriptionResponseDto getCurrentSubscription(Authentication authentication) {
        User user = getCurrentUser(authentication);
        Optional<UserSubscription> subscription = userSubscriptionRepository
                .findFirstByUserIdAndDeletedAtIsNullAndStatusAndEndAtGreaterThanOrderByStartAtDesc(
                        user.getId(),
                        "ACTIVE",
                        LocalDateTime.now()
                );

        if (subscription.isEmpty()) {
            throw new ResourceNotFoundException("No active subscription found for user");
        }

        return mapToResponseDto(subscription.get());
    }

    @Override
    @Transactional
    public void cancelCurrentSubscription(Authentication authentication) {
        User user = getCurrentUser(authentication);
        UserSubscription subscription = userSubscriptionRepository
                .findFirstByUserIdAndDeletedAtIsNullAndStatusAndEndAtGreaterThanOrderByStartAtDesc(
                        user.getId(),
                        "ACTIVE",
                        LocalDateTime.now()
                )
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription found for user"));

        cancelSubscription(user, subscription, LocalDateTime.now());
    }





    private void cancelSubscription(User user, UserSubscription subscription, LocalDateTime now) {
        subscription.setStatus("CANCELLED");
        subscription.setDeletedAt(now);
        subscription.setUpdatedAt(now);
        userSubscriptionRepository.save(subscription);

        SubscriptionHistory history = SubscriptionHistory.builder()
                .user(user)
                .fromPlan(subscription.getPlan())
                .toPlan(null)
                .changedAt(now)
                .build();
        subscriptionHistoryRepository.save(history);

        user.setCurrentPlan(null);
        user.setPlanExpiresAt(null);
        userRepository.save(user);
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void autoCancelExpiredSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        List<UserSubscription> expired = userSubscriptionRepository.findByStatusAndEndAtLessThanEqual("ACTIVE", now);
        for (UserSubscription subscription : expired) {
            User user = subscription.getUser();
            cancelSubscription(user, subscription, now);
        }
    }

    @Override
    public List<SubscriptionResponseDto> getUserSubscriptionHistory(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<UserSubscription> subscriptions = userSubscriptionRepository.findByUserIdAndDeletedAtIsNullOrderByStartAtDesc(user.getId());
        return subscriptions.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private LocalDateTime calculateEndDate(LocalDateTime startDate, String billingCycle) {
        return switch (billingCycle) {
            case "MONTHLY" -> startDate.plusMonths(1);
            case "QUARTERLY" -> startDate.plusMonths(3);
            case "YEARLY", "ANNUAL" -> startDate.plusYears(1);
            case "LIFETIME" -> startDate.plusYears(100); // Effectively lifetime
            default -> startDate.plusMonths(1); // Default to monthly
        };
    }

    private boolean isFamilyPlan(Plan plan) {
        return plan.getName() != null && "FAMILY".equalsIgnoreCase(plan.getName().trim());
    }

    private String resolveClientIp(HttpServletRequest httpRequest) {
        if (httpRequest == null) {
            return null;
        }

        String forwardedFor = httpRequest.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = httpRequest.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        String remoteAddr = httpRequest.getRemoteAddr();
        return remoteAddr != null && !remoteAddr.isBlank() ? remoteAddr.trim() : null;
    }

    private FamilyMemberResponseDto mapToFamilyMemberResponseDto(FamilyMember familyMember) {
        return FamilyMemberResponseDto.builder()
                .id(familyMember.getId())
                .subscriptionId(familyMember.getSubscription().getId())
                .userId(familyMember.getUser().getId())
                .role(familyMember.getRole())
                .createdAt(familyMember.getCreatedAt())
                .build();
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

    @Override
    @Transactional
    public PurchaseVnPayResponse initiateVnPayPurchase(Authentication authentication,
            PurchaseSubscriptionRequest request,
            HttpServletRequest httpRequest) {
        User user = getCurrentUser(authentication);

        // Fetch and validate plan
        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        if (!Boolean.TRUE.equals(plan.getIsActive())) {
            throw new BadRequestException("Only active plans can be purchased");
        }

        LocalDateTime now = LocalDateTime.now();

        // Create pending subscription first because transaction.subscription_id is non-null in DB
        LocalDateTime endAt = calculateEndDate(now, plan.getBillingCycle());
        UserSubscription pendingSubscription = UserSubscription.builder()
                .user(user)
                .plan(plan)
                .status("PENDING")
                .startAt(now)
                .endAt(endAt)
                .createdAt(now)
                .updatedAt(now)
                .build();
        UserSubscription savedSubscription = userSubscriptionRepository.saveAndFlush(pendingSubscription);
        if (savedSubscription.getId() == null) {
            throw new BadRequestException("Failed to create pending subscription for VNPay transaction");
        }

        // Create transaction linked to the pending subscription
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setSubscription(savedSubscription);
        transaction.setAmount(plan.getPrice());
        transaction.setCurrency("VND");
        transaction.setPaymentMethod("VNPAY");
        transaction.setStatus("PENDING");
        transaction.setCreatedAt(now);
        Transaction savedTransaction = transactionRepository.saveAndFlush(transaction);

        // Generate payment URL
        String paymentUrl = vnPayService.createPaymentUrl(savedTransaction, savedSubscription, plan, resolveClientIp(httpRequest));

        return PurchaseVnPayResponse.builder()
                .paymentUrl(paymentUrl)
                .transactionId(savedTransaction.getId())
                .subscriptionId(savedSubscription.getId())
                .build();
    }

    @Override
    @Transactional
    public PurchaseVnPayResponse confirmVnPayPayment(Map<String, String> params) {
        // Validate VNPay signature
        if (!vnPayService.isValidReturnSignature(params)) {
            throw new BadRequestException("Invalid VNPay payment signature");
        }

        // Extract response code and transaction ref
        String responseCode = params.getOrDefault("vnp_ResponseCode", "");
        String transactionRef = params.getOrDefault("vnp_TxnRef", "");

        if (transactionRef.isEmpty()) {
            throw new BadRequestException("Missing transaction reference from VNPay");
        }

        try {
            Long transactionId = Long.parseLong(transactionRef);
            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));

            LocalDateTime now = LocalDateTime.now();
            UserSubscription subscription = transaction.getSubscription();
            User user = transaction.getUser();

            if ("00".equals(responseCode)) {
                if ("SUCCESS".equalsIgnoreCase(transaction.getStatus())
                        && "ACTIVE".equalsIgnoreCase(subscription.getStatus())) {
                    return PurchaseVnPayResponse.builder()
                            .transactionId(transaction.getId())
                            .subscriptionId(subscription.getId())
                            .paymentUrl(null)
                            .build();
                }

                if (!"PENDING".equalsIgnoreCase(subscription.getStatus())) {
                    throw new BadRequestException("Subscription is not pending and cannot be activated");
                }

                // Payment successful
                transaction.setStatus("SUCCESS");
                transaction.setPaidAt(now);
                transactionRepository.save(transaction);

                // Check if user has existing active subscription and handle upgrade
                Optional<UserSubscription> existingSubscription = userSubscriptionRepository
                        .findFirstByUserIdAndDeletedAtIsNullAndStatusAndEndAtGreaterThanOrderByStartAtDesc(
                                user.getId(),
                                "ACTIVE",
                                now
                        );

                if (existingSubscription.isPresent() && !existingSubscription.get().getId().equals(subscription.getId())) {
                    UserSubscription oldSubscription = existingSubscription.get();

                    // Create history record
                    SubscriptionHistory history = SubscriptionHistory.builder()
                            .user(user)
                            .fromPlan(oldSubscription.getPlan())
                            .toPlan(subscription.getPlan())
                            .changedAt(now)
                            .build();
                    subscriptionHistoryRepository.save(history);

                    // Soft delete old subscription
                    oldSubscription.setDeletedAt(now);
                    userSubscriptionRepository.save(oldSubscription);
                }

                // Activate subscription
                subscription.setStatus("ACTIVE");
                subscription.setUpdatedAt(now);
                userSubscriptionRepository.save(subscription);

                // Update user's current plan
                user.setCurrentPlan(subscription.getPlan());
                user.setPlanExpiresAt(subscription.getEndAt());
                userRepository.save(user);

                // Create family member entry if it's a family plan
                if (isFamilyPlan(subscription.getPlan())) {
                    if (!familyMemberRepository.existsBySubscriptionIdAndUserId(subscription.getId(), user.getId())) {
                        FamilyMember familyMember = new FamilyMember();
                        familyMember.setSubscription(subscription);
                        familyMember.setUser(user);
                        familyMember.setRole("OWNER");
                        familyMember.setCreatedAt(now);
                        familyMemberRepository.save(familyMember);
                    }
                }
            } else {
                // Payment failed
                transaction.setStatus("FAILED");
                transactionRepository.save(transaction);

                // Mark subscription as failed
                subscription.setStatus("FAILED");
                subscription.setDeletedAt(now);
                subscription.setUpdatedAt(now);
                userSubscriptionRepository.save(subscription);

                throw new BadRequestException("Payment failed with response code: " + responseCode);
            }

            return PurchaseVnPayResponse.builder()
                    .transactionId(transaction.getId())
                    .subscriptionId(subscription.getId())
                    .paymentUrl(null)
                    .build();
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Invalid transaction reference format");
        }
    }

   
    
}
