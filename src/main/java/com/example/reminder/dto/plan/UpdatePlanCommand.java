package com.example.reminder.dto.plan;

import java.math.BigDecimal;

public record UpdatePlanCommand(
        Long userId,
        String name,
        BigDecimal price,
        String billingCycle,
        Integer maxReminders,
        Integer maxTrustedContacts,
        Integer maxDigitalAssets,
        String features,
        Boolean isActive
) {
}
