package com.example.reminder.domain.enums;

public enum RiskLevel {
    LOW,       // 0 consecutive missed days
    MEDIUM,    // 1 consecutive missed days
    HIGH,      // 2 consecutive missed days
    CRITICAL   // User not responding for extended period (3 days)
}
