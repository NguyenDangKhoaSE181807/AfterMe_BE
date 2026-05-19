package com.example.reminder.dto.subscription;

import java.time.LocalDateTime;
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
public class FamilyMemberResponseDto {

    private Long id;
    private Long subscriptionId;
    private Long userId;
    private String role;
    private LocalDateTime createdAt;
}