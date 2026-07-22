package com.example.reminder.entity;

import com.example.reminder.domain.enums.UserActivitySignalType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_activity_states")
public class UserActivityState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_id", nullable = false, length = 255)
    private String deviceId;

    @Column(name = "last_app_foreground_at")
    private LocalDateTime lastAppForegroundAt;

    @Column(name = "last_app_interaction_at")
    private LocalDateTime lastAppInteractionAt;

    @Column(name = "last_push_tapped_at")
    private LocalDateTime lastPushTappedAt;

    @Column(name = "last_device_unlocked_at")
    private LocalDateTime lastDeviceUnlockedAt;

    @Column(name = "last_device_interactive_at")
    private LocalDateTime lastDeviceInteractiveAt;

    @Column(name = "last_motion_at")
    private LocalDateTime lastMotionAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_activity_type", length = 40)
    private UserActivitySignalType lastActivityType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
