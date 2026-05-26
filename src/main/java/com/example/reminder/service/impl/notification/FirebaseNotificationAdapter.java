package com.example.reminder.service.impl.notification;

import com.example.reminder.domain.model.NotificationMessage;
import com.example.reminder.entity.UserDevice;
import com.example.reminder.repository.UserDeviceRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notification.firebase", name = "enabled", havingValue = "true")
public class FirebaseNotificationAdapter implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(FirebaseNotificationAdapter.class);

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

        log.info("FirebaseNotificationAdapter device lookup: userId={}, deviceCount={}",
                message.userId(),
                devices.size());

        if (devices.isEmpty()) {
            log.info("No notification-enabled devices found for user {}", message.userId());
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
                    .putAllData(toData(message))
                    .build();

            try {
                log.info("FCM send attempt: userId={}, deviceId={}, reminderId={}, instanceId={}",
                    message.userId(),
                    device.getDeviceId(),
                    message.reminderId(),
                    message.instanceId());

                String messageId = firebaseMessaging.send(firebaseMessage);
                log.info("FCM send success: messageId={}, userId={}, deviceId={}, reminderId={}, instanceId={}",
                        messageId,
                        message.userId(),
                        device.getDeviceId(),
                        message.reminderId(),
                        message.instanceId());
            } catch (Exception ex) {
                log.error("Failed to send Firebase message to user {} device {}", message.userId(), device.getDeviceId(), ex);
            }
        }
    }

    private Map<String, String> toData(NotificationMessage message) {
        Map<String, String> data = new HashMap<>();
        putIfNotNull(data, "userId", message.userId());
        putIfNotNull(data, "reminderId", message.reminderId());
        putIfNotNull(data, "scheduleId", message.scheduleId());
        putIfNotNull(data, "instanceId", message.instanceId());
        putIfNotNull(data, "sourceType", message.sourceType());
        putIfNotNull(data, "requiresResponse", message.requiresResponse());
        putIfNotNull(data, "sentAt", message.sentAt());
        return data;
    }

    private void putIfNotNull(Map<String, String> data, String key, Object value) {
        if (value != null) {
            data.put(key, String.valueOf(value));
        }
    }
}




