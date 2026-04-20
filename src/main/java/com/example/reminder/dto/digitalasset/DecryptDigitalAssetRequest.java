package com.example.reminder.dto.digitalasset;

import jakarta.validation.constraints.NotNull;

public record DecryptDigitalAssetRequest(
        @NotNull Long trustedContactId
) {
}
