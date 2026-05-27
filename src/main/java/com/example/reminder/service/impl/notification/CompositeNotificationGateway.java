package com.example.reminder.service.impl.notification;

import com.example.reminder.domain.model.NotificationMessage;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class CompositeNotificationGateway implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(CompositeNotificationGateway.class);

    private final WebSocketNotificationAdapter webSocketNotificationAdapter;
    private final ObjectProvider<FirebaseNotificationAdapter> firebaseNotificationAdapterProvider;

    @Override
    public void send(NotificationMessage message) {
        List<NotificationSender> delegates = new ArrayList<>();
        delegates.add(webSocketNotificationAdapter);

        FirebaseNotificationAdapter firebaseAdapter = firebaseNotificationAdapterProvider.getIfAvailable();
        if (firebaseAdapter != null) {
            delegates.add(firebaseAdapter);
        }

        log.info("CompositeNotificationGateway delegates: userId={}, delegateCount={}, firebaseEnabled={}",
                message.userId(),
                delegates.size(),
                firebaseAdapter != null);

        delegates.forEach(delegate -> delegate.send(message));
    }
}




