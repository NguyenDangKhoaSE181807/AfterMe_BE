package com.example.reminder.infrastructure.notification;

import com.example.reminder.application.port.out.NotificationGateway;
import com.example.reminder.domain.model.NotificationMessage;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class CompositeNotificationGateway implements NotificationGateway {

    private final WebSocketNotificationAdapter webSocketNotificationAdapter;
    private final ObjectProvider<FirebaseNotificationAdapter> firebaseNotificationAdapterProvider;

    @Override
    public void send(NotificationMessage message) {
        List<NotificationGateway> delegates = new ArrayList<>();
        delegates.add(webSocketNotificationAdapter);

        FirebaseNotificationAdapter firebaseAdapter = firebaseNotificationAdapterProvider.getIfAvailable();
        if (firebaseAdapter != null) {
            delegates.add(firebaseAdapter);
        }

        delegates.forEach(delegate -> delegate.send(message));
    }
}




