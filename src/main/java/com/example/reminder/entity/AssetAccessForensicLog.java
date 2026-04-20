package com.example.reminder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "asset_access_forensic_logs")
public class AssetAccessForensicLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempted_asset_id")
    private Long attemptedAssetId;

    @Column(name = "actor_id", length = 100)
    private String actorId;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(name = "reason_code", nullable = false, length = 50)
    private String reasonCode;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "request_path", length = 255)
    private String requestPath;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
