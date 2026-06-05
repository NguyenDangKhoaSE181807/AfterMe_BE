package com.example.reminder.dto.userdevice;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdateUserDeviceLocationRequest(
        @NotNull
        @DecimalMin(value = "-90.0", message = "Latitude must be greater than or equal to -90")
        @DecimalMax(value = "90.0", message = "Latitude must be less than or equal to 90")
        BigDecimal latitude,

        @NotNull
        @DecimalMin(value = "-180.0", message = "Longitude must be greater than or equal to -180")
        @DecimalMax(value = "180.0", message = "Longitude must be less than or equal to 180")
        BigDecimal longitude,

        @Min(value = 1, message = "Accuracy must be positive")
        Integer accuracyMeters,

        LocalDateTime capturedAt,

        String source
) {
}
