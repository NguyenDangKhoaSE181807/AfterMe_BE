package com.example.reminder.dto.digitalasset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDigitalAssetRequest(
        @NotNull Long userId,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 20) String type,
        @NotBlank @Size(max = 255) String identifier,
        @NotBlank @Size(max = 4096) String secret,
        String instructions
) {
}
