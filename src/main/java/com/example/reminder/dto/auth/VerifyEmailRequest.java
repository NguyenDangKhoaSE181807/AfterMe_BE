package com.example.reminder.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyEmailRequest(
        @NotNull(message = "User ID is required")
        Long userId,

        @NotBlank(message = "Verification code is required")
        @Size(min = 8, max = 8, message = "Verification code must be 8 digits")
        @Pattern(regexp = "^\\d{8}$", message = "Verification code must contain exactly 8 digits")
        String code
) {
}
