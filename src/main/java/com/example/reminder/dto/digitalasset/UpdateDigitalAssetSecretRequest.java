package com.example.reminder.dto.digitalasset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDigitalAssetSecretRequest(
        @NotBlank @Size(max = 4096) String secret
) {
}
