package com.example.reminder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.vnpay")
public class VnPayProperties {
    private String tmnCode;
    private String hashSecret;
    private String url;
    private String returnUrl;
}
