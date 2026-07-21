package com.example.reminder.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyEmailRequest(
        @NotNull(message = "User ID is required")
        Long userId,

        @NotBlank(message = "Verification code is required")
        @Size(min = 6, max = 6, message = "Verification code must be 6 digits")
        @Pattern(regexp = "^\\d{6}$", message = "Verification code must contain exactly 6 digits")
        String code
) {
}
