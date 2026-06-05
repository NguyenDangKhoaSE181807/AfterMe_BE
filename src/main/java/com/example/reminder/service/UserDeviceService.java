package com.example.reminder.service;

import com.example.reminder.dto.userdevice.UpdateUserDeviceNotificationRequest;
import com.example.reminder.dto.userdevice.UpdateUserDeviceLocationRequest;
import com.example.reminder.dto.userdevice.UpsertUserDeviceRequest;
import com.example.reminder.dto.userdevice.UserDeviceResponseDto;
import java.util.List;

public interface UserDeviceService {

    UserDeviceResponseDto upsertCurrentDevice(String userEmail, String deviceId, UpsertUserDeviceRequest request);

    List<UserDeviceResponseDto> listMyDevices(String userEmail);

    UserDeviceResponseDto updateCurrentDeviceNotificationEnabled(
            String userEmail,
            String deviceId,
            UpdateUserDeviceNotificationRequest request
    );

    UserDeviceResponseDto updateCurrentDeviceLocation(
            String userEmail,
            String deviceId,
            UpdateUserDeviceLocationRequest request
    );

    void deleteCurrentDevice(String userEmail, String deviceId);
}
