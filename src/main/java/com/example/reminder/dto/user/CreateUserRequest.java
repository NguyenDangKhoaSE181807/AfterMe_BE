package com.example.reminder.dto.user;

import com.example.reminder.domain.enums.TonePreference;
import com.example.reminder.domain.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 255) String passwordHash,
        @NotBlank @Size(max = 255) String fullName,
        @NotNull TonePreference tonePreference,
        @NotNull UserStatus status
) {
}





