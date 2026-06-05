package com.example.reminder.dto.trustedcontact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record CreateTrustedContactRequest(
        @Size(min = 2, max = 255, message = "Full name must be between 2 and 255 characters")
        String fullName,

        @Size(max = 100, message = "Relationship must be maximum 100 characters")
        String relationship,

        @Size(max = 30, message = "Phone must be maximum 30 characters")
        String phone,

        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must be maximum 255 characters")
        String email,

        @Min(value = 1, message = "Priority must be at least 1")
        Integer priority
) {
}
