package com.example.reminder.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordWithCodeRequest(
        @NotBlank(message = "Verification code is required")
        @Size(min = 8, max = 8, message = "Verification code must be 8 digits")
        @Pattern(regexp = "^\\d{8}$", message = "Verification code must contain exactly 8 digits")
        String code,

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
            message = "Password must contain at least one lowercase letter, one uppercase letter, and one digit"
        )
        String newPassword
) {
}
