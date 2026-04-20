package com.example.reminder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "asset_access_logs")
public class AssetAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "digital_asset_id", nullable = false)
    private DigitalAsset digitalAsset;

    @Column(name = "accessed_by", nullable = false, length = 100)
    private String accessedBy;

    @Column(nullable = false, length = 20)
    private String action;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "reason_code", length = 50)
    private String reasonCode;

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