package com.example.reminder.application.usecase;

import com.example.reminder.application.dto.notification.SendNotificationRequest;
import com.example.reminder.application.port.out.NotificationGateway;
import com.example.reminder.domain.model.NotificationMessage;
import java.time.LocalDateTime;
public class SendNotificationUseCase {

    private final NotificationGateway notificationGateway;

    public SendNotificationUseCase(NotificationGateway notificationGateway) {
        this.notificationGateway = notificationGateway;
    }

    public void execute(SendNotificationRequest request) {
        NotificationMessage message = new NotificationMessage(
                request.userId(),
                request.title(),
                request.body(),
                LocalDateTime.now()
        );

        notificationGateway.send(message);
    }
}




