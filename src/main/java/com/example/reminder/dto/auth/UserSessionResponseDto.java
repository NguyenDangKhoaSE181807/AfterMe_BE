package com.example.reminder.dto.auth;

import com.example.reminder.domain.enums.SessionStatus;
import com.example.reminder.entity.UserSession;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserSessionResponseDto {
    private Long id;
    private String deviceId;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime expiresAt;
    private SessionStatus status;
    private boolean isCurrent;

    public static UserSessionResponseDto from(UserSession session, boolean isCurrent) {
        UserSessionResponseDto dto = new UserSessionResponseDto();
        dto.setId(session.getId());
        dto.setDeviceId(session.getDeviceId());
        dto.setIpAddress(session.getIpAddress());
        dto.setUserAgent(session.getUserAgent());
        dto.setCreatedAt(session.getCreatedAt());
        dto.setLastUsedAt(session.getLastUsedAt());
        dto.setExpiresAt(session.getExpiresAt());
        dto.setStatus(session.getStatus());
        dto.setCurrent(isCurrent);
        return dto;
    }
}
