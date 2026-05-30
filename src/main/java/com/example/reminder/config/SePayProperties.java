package com.example.reminder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.sepay")
public class SePayProperties {
    private String bankCode;
    private String accountNumber;
    private String accountName;
    private String webhookSecret;
    private String transferPrefix = "AFM";
}
