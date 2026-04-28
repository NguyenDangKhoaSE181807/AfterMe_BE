package com.example.reminder.dto.plan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdatePlanRequest(
        @NotNull Long userId,
        @NotBlank @Size(max = 50) String name,
        @NotNull BigDecimal price,
        @NotBlank @Size(max = 20) String billingCycle,
        @NotNull Integer maxReminders,
        @NotNull Integer maxTrustedContacts,
        @NotNull Integer maxDigitalAssets,
        String features,
        Boolean isActive
) {
}
