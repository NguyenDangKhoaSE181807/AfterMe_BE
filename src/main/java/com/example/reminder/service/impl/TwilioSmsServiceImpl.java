package com.example.reminder.service.impl;

import com.example.reminder.config.SmsProperties;
import com.example.reminder.exception.BadRequestException;
import com.example.reminder.service.SmsService;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.sms", name = "enabled", havingValue = "true")
public class TwilioSmsServiceImpl implements SmsService {

    private static final String TWILIO_MESSAGES_URL = "https://api.twilio.com/2010-04-01/Accounts/{accountSid}/Messages.json";

    private final SmsProperties smsProperties;
    private final RestClient.Builder restClientBuilder;

    @Override
    public void sendSafetyAlertSms(String recipientPhone, String message) {
        SmsProperties.Twilio twilio = smsProperties.twilio();
        validateTwilioConfig(twilio);

        String normalizedPhone = normalizePhone(recipientPhone);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", normalizedPhone);
        form.add("Body", message);

        if (hasText(twilio.messagingServiceSid())) {
            form.add("MessagingServiceSid", twilio.messagingServiceSid().trim());
        } else {
            form.add("From", normalizePhone(twilio.fromPhone()));
        }

        log.info("Twilio SMS send attempt: accountSid={}, to={}, from={}, messagingServiceSidPresent={}",
                maskSid(twilio.accountSid()),
                normalizedPhone,
                form.getFirst("From"),
                hasText(twilio.messagingServiceSid()));

        Map<?, ?> response = restClientBuilder.build()
                .post()
                .uri(TWILIO_MESSAGES_URL, twilio.accountSid().trim())
                .headers(headers -> {
                    headers.setBasicAuth(twilio.accountSid().trim(), twilio.authToken().trim(), StandardCharsets.UTF_8);
                    headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                })
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        Object messageSid = response == null ? null : response.get("sid");
        log.info("Twilio SMS sent: to={}, sid={}", normalizedPhone, messageSid);
    }

    private void validateTwilioConfig(SmsProperties.Twilio twilio) {
        if (twilio == null) {
            throw new IllegalStateException("Twilio SMS config is missing");
        }
        if (!hasText(twilio.accountSid())) {
            throw new IllegalStateException("Twilio account SID is missing");
        }
        if (!hasText(twilio.authToken())) {
            throw new IllegalStateException("Twilio auth token is missing");
        }
        if (!hasText(twilio.fromPhone()) && !hasText(twilio.messagingServiceSid())) {
            throw new IllegalStateException("Twilio from phone or messaging service SID is required");
        }
    }

    private String normalizePhone(String phone) {
        if (!hasText(phone)) {
            throw new BadRequestException("Recipient phone is required");
        }
        String normalized = phone.trim().replaceAll("[\\s().-]", "");
        if (normalized.startsWith("+")) {
            return normalized;
        }
        if (normalized.startsWith("84")) {
            return "+" + normalized;
        }
        if (normalized.startsWith("0")) {
            return "+84" + normalized.substring(1);
        }
        return "+" + normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String maskSid(String sid) {
        if (!hasText(sid) || sid.length() <= 8) {
            return "****";
        }
        return sid.substring(0, 4) + "..." + sid.substring(sid.length() - 4);
    }
}
