package com.example.reminder.dto.auth;

import com.example.reminder.domain.enums.TonePreference;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record SignUpRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        @Size(min = 5, max = 255, message = "Email must be between 5 and 255 characters")
        String email,
        
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
            message = "Password must contain at least one lowercase letter, one uppercase letter, and one digit"
        )
        String password,
        
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 255, message = "Full name must be between 2 and 255 characters")
        @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "Full name can only contain letters, spaces, hyphens, and apostrophes")
        String fullName,
        
        TonePreference tonePreference
) {
}
