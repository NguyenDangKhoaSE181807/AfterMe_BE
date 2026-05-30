package com.example.reminder.controller;

import com.example.reminder.dto.common.BaseResponse;
import com.example.reminder.dto.subscription.PurchaseVnPayResponse;
import com.example.reminder.service.SubscriptionService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
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

    @PostMapping("/sepay/webhook")
    public ResponseEntity<BaseResponse<Map<String, Object>>> sePayWebhook(
            @RequestBody(required = false) String payload,
            @RequestHeader(value = "X-SePay-Signature", required = false) String signature,
            @RequestHeader(value = "X-SePay-Timestamp", required = false) String timestamp
    ) {
        payload = payload == null ? "" : payload;
        log.info("SePay webhook received - signature=[{}] timestamp=[{}] payload=[{}]", signature, timestamp, payload);
        subscriptionService.handleSePayWebhook(payload, signature, timestamp);

        Map<String, Object> data = new HashMap<>();
        data.put("received", true);

        BaseResponse<Map<String, Object>> response = BaseResponse.<Map<String, Object>>builder()
                .success(true)
                .code("SEPAY_WEBHOOK_RECEIVED")
                .message("SePay webhook processed")
                .data(data)
                .timestamp(java.time.Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
