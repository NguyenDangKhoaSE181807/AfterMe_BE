package com.example.reminder.service.impl;

import com.example.reminder.domain.enums.ActivityLogType;
import com.example.reminder.dto.notification.SendNotificationRequest;
import com.example.reminder.domain.model.NotificationMessage;
import com.example.reminder.service.ActivityLogService;
import com.example.reminder.service.impl.notification.NotificationSender;
import com.example.reminder.service.NotificationService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationSender notificationSender;
    private final ActivityLogService activityLogService;

    @Override
    public void send(SendNotificationRequest request) {
        log.info("NotificationService send request: userId={}, reminderId={}, instanceId={}, sourceType={}, requiresResponse={}",
            request.userId(),
            request.reminderId(),
            request.instanceId(),
            request.sourceType(),
            request.requiresResponse());

        NotificationMessage message = new NotificationMessage(
                request.userId(),
                request.title(),
                request.body(),
                LocalDateTime.now(),
                request.reminderId(),
                request.scheduleId(),
                request.instanceId(),
                request.sourceType(),
                request.requiresResponse()
        );

        log.info("NotificationService built message: userId={}, reminderId={}, instanceId={}",
                message.userId(),
                message.reminderId(),
                message.instanceId());

        notificationSender.send(message);
        activityLogService.record(
                request.userId(),
                request.requiresResponse() != null && request.requiresResponse()
                        ? ActivityLogType.ALERT_RECEIVED
                        : ActivityLogType.NOTIFICATION_RECEIVED,
                request.title(),
                request.body(),
                request.reminderId(),
                request.scheduleId(),
                request.instanceId(),
                null
        );
    }
}
