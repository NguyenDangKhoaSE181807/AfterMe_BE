package com.example.reminder.service;

import com.example.reminder.dto.securitypin.UserPinStatusResponse;

public interface UserPinService {

    void setPin(Long userId, String pin);

    void verifyPin(Long userId, String pin);

    UserPinStatusResponse getStatus(Long userId);
}
