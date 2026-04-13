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
@Table(name = "asset_shares")
public class AssetShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "digital_asset_id", nullable = false)
    private DigitalAsset digitalAsset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trusted_contact_id", nullable = false)
    private TrustedContact trustedContact;

    @Column(name = "unlock_condition", nullable = false, length = 30)
    private String unlockCondition;

    @Column(name = "is_unlocked", nullable = false)
    private Boolean isUnlocked = false;

    @Column(name = "unlock_status", nullable = false, length = 20)
    private String unlockStatus = "LOCKED";

    @Column(name = "unlocked_by", length = 20)
    private String unlockedBy;

    @Column(name = "unlock_delay_hours")
    private Integer unlockDelayHours;

    @Column(name = "unlock_policy", columnDefinition = "TEXT")
    private String unlockPolicy;

    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
