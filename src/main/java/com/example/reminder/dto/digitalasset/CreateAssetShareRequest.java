package com.example.reminder.dto.digitalasset;

import jakarta.validation.constraints.NotNull;

public record CreateAssetShareRequest(
        @NotNull Long trustedContactId,
        String unlockCondition,
        Boolean unlockNow,
        Integer unlockDelayHours,
        String unlockPolicy
) {
}
