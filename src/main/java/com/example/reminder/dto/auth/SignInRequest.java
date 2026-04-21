package com.example.reminder.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignInRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        @Size(min = 5, max = 255, message = "Email must be between 5 and 255 characters")
        String email,
        
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters")
        String password
) {
}
