package com.example.reminder.dto.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionHistoryResponseDto {

    private Long id;

    private Long userId;

    private Long fromPlanId;

    private String fromPlanName;

    private Long toPlanId;

    private String toPlanName;

    private LocalDateTime changedAt;
}
