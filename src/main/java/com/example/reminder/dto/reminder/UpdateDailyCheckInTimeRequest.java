package com.example.reminder.dto.reminder;

import com.example.reminder.config.FlexibleLocalTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record UpdateDailyCheckInTimeRequest(
        @NotNull(message = "dailyCheckInTime is required")
        @JsonDeserialize(using = FlexibleLocalTimeDeserializer.class)
        @Schema(type = "string", example = "20:00:00", description = "Daily check-in time.")
        LocalTime dailyCheckInTime
) {
}
