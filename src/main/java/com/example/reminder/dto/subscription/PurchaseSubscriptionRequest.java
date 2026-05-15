package com.example.reminder.dto.subscription;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseSubscriptionRequest {

    @NotNull(message = "Plan ID is required")
    private Long planId;

    private Boolean autoRenew = true;
}
