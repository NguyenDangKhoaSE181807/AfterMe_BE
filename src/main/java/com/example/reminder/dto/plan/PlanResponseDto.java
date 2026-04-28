package com.example.reminder.dto.plan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PlanResponseDto(
        Long id,
        String name,
        BigDecimal price,
        String billingCycle,
        Integer maxReminders,
        Integer maxTrustedContacts,
        Integer maxDigitalAssets,
        String features,
        Boolean isActive,
        LocalDateTime createdAt
) {
}
