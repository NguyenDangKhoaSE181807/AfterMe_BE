package com.example.reminder.dto.trustedcontact;

import jakarta.validation.constraints.NotNull;

public record UpdateTrustedContactRequest(
        String fullName,
        String relationship,
        String phone,
        String email,
        @NotNull(message = "isActive must be specified")
        Boolean isActive
) {
}
