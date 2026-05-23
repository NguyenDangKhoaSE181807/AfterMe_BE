package com.example.reminder.dto.securitypin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyUserPinRequest(
        @NotBlank
        @Size(min = 6, max = 6)
        @Pattern(regexp = "^[0-9]+$", message = "PIN must be numeric")
        String pin
) {
}
