package com.example.reminder.controller;

import com.example.reminder.dto.subscription.AddFamilyMemberRequest;
import com.example.reminder.dto.subscription.FamilyMemberResponseDto;
import com.example.reminder.dto.subscription.PurchaseSePayResponse;
import com.example.reminder.dto.subscription.PurchaseSubscriptionRequest;
import com.example.reminder.dto.subscription.PurchaseVnPayResponse;
import com.example.reminder.dto.subscription.SubscriptionResponseDto;
import com.example.reminder.dto.subscription.UserSubscriptionDto;
import com.example.reminder.dto.common.BaseResponse;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.service.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/purchase")
    public ResponseEntity<BaseResponse<SubscriptionResponseDto>> purchaseSubscription(
            @Valid @RequestBody PurchaseSubscriptionRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        SubscriptionResponseDto subscription = subscriptionService.purchaseSubscription(authentication, request);

        BaseResponse<SubscriptionResponseDto> response = BaseResponse.<SubscriptionResponseDto>builder()
                .code("PURCHASE_SUBSCRIPTION_SUCCESS")
                .message("Subscription purchased successfully")
                .data(subscription)
                .timestamp(java.time.Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

        @PostMapping("/purchase/vnpay")
        public ResponseEntity<BaseResponse<PurchaseVnPayResponse>> purchaseSubscriptionVnPay(
                        @Valid @RequestBody PurchaseSubscriptionRequest request,
                        Authentication authentication,
                        HttpServletRequest httpRequest
        ) {
                PurchaseVnPayResponse resp = subscriptionService.initiateVnPayPurchase(authentication, request, httpRequest);

                BaseResponse<PurchaseVnPayResponse> response = BaseResponse.<PurchaseVnPayResponse>builder()
                                .code("PURCHASE_SUBSCRIPTION_VNPAY_INITIATED")
                                .message("VNPay payment initiated")
                                .data(resp)
                                .timestamp(java.time.Instant.now())
                                .build();

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

                @PostMapping("/purchase/sepay")
                public ResponseEntity<BaseResponse<PurchaseSePayResponse>> purchaseSubscriptionSePay(
                                @Valid @RequestBody PurchaseSubscriptionRequest request,
                                Authentication authentication
                ) {
                        PurchaseSePayResponse resp = subscriptionService.initiateSePayPurchase(authentication, request);

                        BaseResponse<PurchaseSePayResponse> response = BaseResponse.<PurchaseSePayResponse>builder()
                                        .success(true)
                                        .code("PURCHASE_SUBSCRIPTION_SEPAY_INITIATED")
                                        .message("SePay payment initiated")
                                        .data(resp)
                                        .timestamp(java.time.Instant.now())
                                        .build();

                        return ResponseEntity.status(HttpStatus.CREATED).body(response);
                }

        @PostMapping("/family-members")
        public ResponseEntity<BaseResponse<FamilyMemberResponseDto>> addFamilyMember(
                        @Valid @RequestBody AddFamilyMemberRequest request,
                        Authentication authentication,
                        HttpServletRequest httpRequest
        ) {
                FamilyMemberResponseDto familyMember = subscriptionService.addFamilyMember(authentication, request);

                BaseResponse<FamilyMemberResponseDto> response = BaseResponse.<FamilyMemberResponseDto>builder()
                                .success(true)
                                .code("ADD_FAMILY_MEMBER_SUCCESS")
                                .message("Family member added successfully")
                                .data(familyMember)
                                .timestamp(java.time.Instant.now())
                                .build();

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

    @GetMapping("/current")
    public ResponseEntity<BaseResponse<SubscriptionResponseDto>> getCurrentSubscription(
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        SubscriptionResponseDto subscription = subscriptionService.getCurrentSubscription(authentication);

        BaseResponse<SubscriptionResponseDto> response = BaseResponse.<SubscriptionResponseDto>builder()
                .code("GET_SUBSCRIPTION_SUCCESS")
                .message("Get current subscription successfully")
                .data(subscription)
                .timestamp(java.time.Instant.now())
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/current")
    public ResponseEntity<BaseResponse<Void>> cancelCurrentSubscription(
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        subscriptionService.cancelCurrentSubscription(authentication);

        BaseResponse<Void> response = BaseResponse.<Void>builder()
                .code("CANCEL_SUBSCRIPTION_SUCCESS")
                .message("Subscription cancelled successfully")
                .data(null)
                .timestamp(java.time.Instant.now())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<BaseResponse<List<SubscriptionResponseDto>>> getSubscriptionHistory(
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        List<SubscriptionResponseDto> subscriptions = subscriptionService.getUserSubscriptionHistory(authentication);

        BaseResponse<List<SubscriptionResponseDto>> response = BaseResponse.<List<SubscriptionResponseDto>>builder()
                .code("GET_SUBSCRIPTION_HISTORY_SUCCESS")
                .message("Get subscription history successfully")
                .data(subscriptions)
                .timestamp(java.time.Instant.now())
                .build();

        return ResponseEntity.ok(response);
    }

        @GetMapping("/admin")
        public ResponseEntity<BaseResponse<List<SubscriptionResponseDto>>> getAllSubscriptionsForAdmin(
                        Authentication authentication
        ) {
                List<SubscriptionResponseDto> subscriptions = subscriptionService.getAllSubscriptions(authentication);

                BaseResponse<List<SubscriptionResponseDto>> response = BaseResponse.<List<SubscriptionResponseDto>>builder()
                                .code("GET_ALL_SUBSCRIPTIONS_SUCCESS")
                                .message("All subscriptions retrieved successfully")
                                .data(subscriptions)
                                .timestamp(java.time.Instant.now())
                                .build();

                return ResponseEntity.ok(response);
        }

        @GetMapping("/{id}")
        public ResponseEntity<BaseResponse<UserSubscriptionDto>> getSubscriptionById(
                        @PathVariable Long id,
                        HttpServletRequest httpRequest
        ) {
                UserSubscriptionDto subscription = subscriptionService.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + id));

                BaseResponse<UserSubscriptionDto> response = BaseResponse.<UserSubscriptionDto>builder()
                                .code("GET_SUBSCRIPTION_BY_ID_SUCCESS")
                                .message("Get subscription by id successfully")
                                .data(subscription)
                                .timestamp(java.time.Instant.now())
                                .build();

                return ResponseEntity.ok(response);
        }

   
}
