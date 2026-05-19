package com.example.reminder.dto.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VnPayPaymentResult {
    private boolean success;
    private Long transactionId;
    private Long subscriptionId;
    private String status;
    private String message;
}
