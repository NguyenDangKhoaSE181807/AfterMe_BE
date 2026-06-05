package com.example.reminder.controller;

import com.example.reminder.dto.common.BaseResponse;
import com.example.reminder.dto.userdevice.UpdateUserDeviceLocationRequest;
import com.example.reminder.dto.userdevice.UpdateUserDeviceNotificationRequest;
import com.example.reminder.dto.userdevice.UpsertUserDeviceRequest;
import com.example.reminder.dto.userdevice.UserDeviceResponseDto;
import com.example.reminder.entity.User;
import com.example.reminder.exception.BadRequestException;
import com.example.reminder.exception.ForbiddenException;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.UserDeviceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-devices")
@RequiredArgsConstructor
public class UserDeviceController {

    private final UserDeviceService userDeviceService;
    private final UserRepository userRepository;

    @PostMapping("/current")
    public ResponseEntity<BaseResponse<UserDeviceResponseDto>> upsertCurrentDevice(
            Authentication authentication,
            HttpServletRequest request,
            @Valid @RequestBody UpsertUserDeviceRequest body
    ) {
        User requester = getCurrentUser(authentication);
        String deviceId = resolveDeviceId(request);
        UserDeviceResponseDto data = userDeviceService.upsertCurrentDevice(requester.getEmail(), deviceId, body);
        return ResponseEntity.ok(buildSuccessResponse("USER_DEVICE_UPSERTED", "Device registered successfully", data, request));
    }

    @GetMapping("/me")
    public ResponseEntity<BaseResponse<List<UserDeviceResponseDto>>> listMyDevices(
            Authentication authentication,
            HttpServletRequest request
    ) {
        User requester = getCurrentUser(authentication);
        List<UserDeviceResponseDto> data = userDeviceService.listMyDevices(requester.getEmail());
        return ResponseEntity.ok(buildSuccessResponse("USER_DEVICE_LIST_FOUND", "User devices retrieved successfully", data, request));
    }

    @PatchMapping("/current/notification-enabled")
    public ResponseEntity<BaseResponse<UserDeviceResponseDto>> updateCurrentDeviceNotificationEnabled(
            Authentication authentication,
            HttpServletRequest request,
            @Valid @RequestBody UpdateUserDeviceNotificationRequest body
    ) {
        User requester = getCurrentUser(authentication);
        String deviceId = resolveDeviceId(request);
        UserDeviceResponseDto data = userDeviceService.updateCurrentDeviceNotificationEnabled(requester.getEmail(), deviceId, body);
        return ResponseEntity.ok(buildSuccessResponse("USER_DEVICE_NOTIFICATION_UPDATED", "Device notification setting updated", data, request));
    }

    @PatchMapping("/current/location")
    public ResponseEntity<BaseResponse<UserDeviceResponseDto>> updateCurrentDeviceLocation(
            Authentication authentication,
            HttpServletRequest request,
            @Valid @RequestBody UpdateUserDeviceLocationRequest body
    ) {
        User requester = getCurrentUser(authentication);
        String deviceId = resolveDeviceId(request);
        UserDeviceResponseDto data = userDeviceService.updateCurrentDeviceLocation(requester.getEmail(), deviceId, body);
        return ResponseEntity.ok(buildSuccessResponse("USER_DEVICE_LOCATION_UPDATED", "Device location updated", data, request));
    }

    @DeleteMapping("/current")
    public ResponseEntity<BaseResponse<Void>> deleteCurrentDevice(
            Authentication authentication,
            HttpServletRequest request
    ) {
        User requester = getCurrentUser(authentication);
        String deviceId = resolveDeviceId(request);
        userDeviceService.deleteCurrentDevice(requester.getEmail(), deviceId);
        return ResponseEntity.ok(buildSuccessResponse("USER_DEVICE_DELETED", "Device removed successfully", null, request));
    }

    private <T> BaseResponse<T> buildSuccessResponse(String code, String message, T data, HttpServletRequest request) {
        return BaseResponse.<T>builder()
                .success(true)
                .code(code)
                .message(message)
                .data(data)
                .errors(null)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .requestId(request.getHeader("X-Request-Id"))
                .build();
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenException("User must be authenticated");
        }

        String email = authentication.getName();
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private String resolveDeviceId(HttpServletRequest request) {
        String headerDeviceId = request.getHeader("X-Device-Id");
        if (headerDeviceId == null || headerDeviceId.isBlank()) {
            throw new BadRequestException("X-Device-Id header is required");
        }
        return headerDeviceId.trim();
    }
}
