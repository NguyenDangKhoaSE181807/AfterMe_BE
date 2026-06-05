package com.example.reminder.service;

public interface SmsService {

    void sendSafetyAlertSms(String recipientPhone, String message);
}
