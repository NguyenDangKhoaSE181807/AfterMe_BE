package com.example.reminder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sms")
public record SmsProperties(
        boolean enabled,
        Twilio twilio
) {

    public record Twilio(
            String accountSid,
            String authToken,
            String fromPhone,
            String messagingServiceSid
    ) {
    }
}
