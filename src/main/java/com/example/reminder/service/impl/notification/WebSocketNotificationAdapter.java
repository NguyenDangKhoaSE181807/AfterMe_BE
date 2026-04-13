package com.example.reminder.service.impl.notification;

import com.example.reminder.domain.model.NotificationMessage;
import com.example.reminder.config.NotificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketNotificationAdapter implements NotificationSender {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationProperties notificationProperties;

    @Override
    public void send(NotificationMessage message) {
        String destination = notificationProperties.websocket().topicPrefix() + "/notifications/" + message.userId();
        messagingTemplate.convertAndSend(destination, message);
    }
}




