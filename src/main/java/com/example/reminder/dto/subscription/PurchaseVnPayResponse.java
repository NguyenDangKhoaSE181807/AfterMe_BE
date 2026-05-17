package com.example.reminder.dto.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseVnPayResponse {
    private String paymentUrl;
    private Long transactionId;
    private Long subscriptionId;
}
