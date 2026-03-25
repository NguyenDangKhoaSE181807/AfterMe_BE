package com.example.reminder.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notification")
public record NotificationProperties(
        WebSocket websocket,
        Firebase firebase
) {

    public record WebSocket(
            String endpoint,
            String appPrefix,
            String topicPrefix
    ) {
    }

    public record Firebase(
            boolean enabled,
            String serviceAccountPath
    ) {
    }
}




