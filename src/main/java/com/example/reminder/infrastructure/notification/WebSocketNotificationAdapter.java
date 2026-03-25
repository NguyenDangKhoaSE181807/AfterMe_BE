package com.example.reminder.infrastructure.notification;

import com.example.reminder.application.port.out.NotificationGateway;
import com.example.reminder.domain.model.NotificationMessage;
import com.example.reminder.infrastructure.config.NotificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketNotificationAdapter implements NotificationGateway {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationProperties notificationProperties;

    @Override
    public void send(NotificationMessage message) {
        String destination = notificationProperties.websocket().topicPrefix() + "/notifications/" + message.userId();
        messagingTemplate.convertAndSend(destination, message);
    }
}




