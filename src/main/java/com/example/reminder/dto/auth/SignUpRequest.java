package com.example.reminder.dto.auth;

import com.example.reminder.domain.enums.TonePreference;
import com.example.reminder.config.FlexibleLocalTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

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
        
        TonePreference tonePreference,

        @JsonDeserialize(using = FlexibleLocalTimeDeserializer.class)
        @Schema(type = "string", example = "20:00:00", description = "Daily check-in time. If omitted, defaults to 20:00.")
        LocalTime dailyCheckInTime
) {
}
