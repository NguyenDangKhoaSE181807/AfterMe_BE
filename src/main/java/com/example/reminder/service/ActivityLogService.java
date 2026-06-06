package com.example.reminder.service;

import com.example.reminder.domain.enums.ActivityLogType;
import com.example.reminder.dto.activity.ActivityLogResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ActivityLogService {

    Page<ActivityLogResponseDto> findCurrentUserLogs(Long userId, Pageable pageable);

    ActivityLogResponseDto record(
            Long userId,
            ActivityLogType type,
            String title,
            String message,
            Long reminderId,
            Long scheduleId,
            Long instanceId,
            String metadata
    );
}
