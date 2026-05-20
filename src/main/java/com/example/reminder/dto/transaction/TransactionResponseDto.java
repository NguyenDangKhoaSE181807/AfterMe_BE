package com.example.reminder.dto.transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponseDto(
        Long id,
        Long userId,
        Long subscriptionId,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String status,
        String transactionRef,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
}
