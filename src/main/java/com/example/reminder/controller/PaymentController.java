package com.example.reminder.controller;

import com.example.reminder.dto.common.BaseResponse;
import com.example.reminder.dto.subscription.PurchaseVnPayResponse;
import com.example.reminder.service.SubscriptionService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/vnpay/return")
    public ResponseEntity<BaseResponse<PurchaseVnPayResponse>> vnPayReturn(@RequestParam Map<String, String> params) {
        PurchaseVnPayResponse result = subscriptionService.confirmVnPayPayment(params);

        BaseResponse<PurchaseVnPayResponse> response = BaseResponse.<PurchaseVnPayResponse>builder()
                .success(true)
                .code("VN_PAY_PAYMENT_CONFIRMED")
                .message("VNPay payment confirmed successfully")
                .data(result)
                .timestamp(java.time.Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
