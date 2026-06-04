package com.example.reminder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Entity
@Table(
        name = "user_devices",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "device_id"})
)
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @Column(name = "fcm_token", length = 1024)
    private String fcmToken;

    @Column(name = "platform", length = 32)
    private String platform;

    @Column(name = "is_trusted", nullable = false)
    private Boolean isTrusted;

    @Column(name = "notification_enabled", nullable = false)
    private Boolean notificationEnabled;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @PrePersist
    @PreUpdate
    private void applyDefaults() {
        if (isTrusted == null) {
            isTrusted = Boolean.FALSE;
        }
        if (notificationEnabled == null) {
            notificationEnabled = Boolean.TRUE;
        }
    }
}