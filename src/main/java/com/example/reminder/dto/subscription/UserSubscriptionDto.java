package com.example.reminder.dto.subscription;

import com.example.reminder.dto.plan.PlanResponseDto;
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
public class UserSubscriptionDto {

    private Long id;

    private Long userId;

    private PlanResponseDto plan;

    private String status;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private Boolean autoRenew;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
