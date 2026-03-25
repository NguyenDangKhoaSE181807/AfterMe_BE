package com.example.reminder.infrastructure.notification;

import com.example.reminder.application.port.out.NotificationGateway;
import com.example.reminder.domain.model.NotificationMessage;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notification.firebase", name = "enabled", havingValue = "true")
public class FirebaseNotificationAdapter implements NotificationGateway {

    private final ObjectProvider<FirebaseApp> firebaseAppProvider;

    @Override
    public void send(NotificationMessage message) {
        FirebaseApp firebaseApp = firebaseAppProvider.getIfAvailable();
        if (firebaseApp == null) {
            log.warn("Firebase is enabled but FirebaseApp is not initialized");
            return;
        }

        Message firebaseMessage = Message.builder()
                .setTopic("user-" + message.userId())
                .setNotification(Notification.builder()
                        .setTitle(message.title())
                        .setBody(message.body())
                        .build())
                .build();

        try {
            FirebaseMessaging.getInstance(firebaseApp).send(firebaseMessage);
        } catch (Exception ex) {
            log.error("Failed to send Firebase message", ex);
        }
    }
}




