package com.example.reminder.dto.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseSePayResponse {
    private Long transactionId;
    private Long subscriptionId;
    private String transferContent;
    private String qrUrl;
    private String bankCode;
    private String accountNumber;
    private String accountName;
    private java.math.BigDecimal amount;
}
