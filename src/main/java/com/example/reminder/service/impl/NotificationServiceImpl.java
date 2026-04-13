package com.example.reminder.service.impl;

import com.example.reminder.dto.notification.SendNotificationRequest;
import com.example.reminder.domain.model.NotificationMessage;
import com.example.reminder.service.impl.notification.NotificationSender;
import com.example.reminder.service.NotificationService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationSender notificationSender;

    @Override
    public void send(SendNotificationRequest request) {
        NotificationMessage message = new NotificationMessage(
                request.userId(),
                request.title(),
                request.body(),
                LocalDateTime.now()
        );

        notificationSender.send(message);
    }
}
