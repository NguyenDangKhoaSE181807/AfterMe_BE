package com.example.reminder.dto.digitalasset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateDigitalAssetRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 20) String type,
        @NotBlank @Size(max = 255) String identifier,
        String instructions,
        @NotNull Boolean isActive
) {
}
