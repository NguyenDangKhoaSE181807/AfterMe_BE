package com.example.reminder.service.impl;

import com.example.reminder.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnMissingBean(SmsService.class)
public class LoggingSmsServiceImpl implements SmsService {

    @Override
    public void sendSafetyAlertSms(String recipientPhone, String message) {
        if (recipientPhone == null || recipientPhone.isBlank()) {
            throw new IllegalArgumentException("Recipient phone is required");
        }
        log.info("SMS safety alert queued: recipientPhone={}, message={}", recipientPhone, message);
    }
}
