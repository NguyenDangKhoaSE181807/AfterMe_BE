package com.example.reminder.service.impl.notification;

import com.example.reminder.domain.model.NotificationMessage;
import com.example.reminder.entity.UserDevice;
import com.example.reminder.repository.UserDeviceRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notification.firebase", name = "enabled", havingValue = "true")
public class FirebaseNotificationAdapter implements NotificationSender {

    private final ObjectProvider<FirebaseApp> firebaseAppProvider;
    private final UserDeviceRepository userDeviceRepository;

    @Override
    public void send(NotificationMessage message) {
        FirebaseApp firebaseApp = firebaseAppProvider.getIfAvailable();
        if (firebaseApp == null) {
            log.warn("Firebase is enabled but FirebaseApp is not initialized");
            return;
        }

        List<UserDevice> devices = userDeviceRepository
            .findByUser_IdAndNotificationEnabledTrueAndFcmTokenIsNotNull(message.userId());

        if (devices.isEmpty()) {
            log.debug("No notification-enabled devices found for user {}", message.userId());
            return;
        }

        FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance(firebaseApp);
        for (UserDevice device : devices) {
            String fcmToken = device.getFcmToken();
            if (fcmToken == null || fcmToken.isBlank()) {
                continue;
            }

            Message firebaseMessage = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(message.title())
                            .setBody(message.body())
                            .build())
                    .build();

            try {
                firebaseMessaging.send(firebaseMessage);
            } catch (Exception ex) {
                log.error("Failed to send Firebase message to user {} device {}", message.userId(), device.getDeviceId(), ex);
            }
        }
    }
}




