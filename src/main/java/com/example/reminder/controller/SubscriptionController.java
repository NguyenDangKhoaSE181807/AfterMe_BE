package com.example.reminder.controller;

import com.example.reminder.dto.subscription.PurchaseSubscriptionRequest;
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
