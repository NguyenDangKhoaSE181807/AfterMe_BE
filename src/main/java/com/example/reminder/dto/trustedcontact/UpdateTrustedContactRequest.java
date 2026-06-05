package com.example.reminder.dto.trustedcontact;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

public record UpdateTrustedContactRequest(
        String fullName,
        String relationship,
        String phone,
        String email,
        @Min(value = 1, message = "Priority must be at least 1")
        Integer priority,
        @NotNull(message = "isActive must be specified")
        Boolean isActive
) {
}
