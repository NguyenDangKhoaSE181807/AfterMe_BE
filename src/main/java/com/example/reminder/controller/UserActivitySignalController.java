package com.example.reminder.controller;

import com.example.reminder.dto.activity.PassiveActivitySettingsRequest;
import com.example.reminder.dto.activity.PassiveActivitySettingsResponseDto;
import com.example.reminder.dto.activity.UserActivitySignalRequest;
import com.example.reminder.dto.activity.UserActivityStateResponseDto;
import com.example.reminder.dto.common.BaseResponse;
import com.example.reminder.entity.User;
import com.example.reminder.exception.ForbiddenException;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.PassiveActivityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-activity-signals")
@RequiredArgsConstructor
public class UserActivitySignalController {

    private final PassiveActivityService passiveActivityService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<BaseResponse<UserActivityStateResponseDto>> recordSignal(
            @Valid @RequestBody UserActivitySignalRequest body,
            Authentication authentication,
            HttpServletRequest request
    ) {
        User requester = getCurrentUser(authentication);
        UserActivityStateResponseDto data = passiveActivityService.recordSignal(requester.getId(), body);
        return ResponseEntity.ok(buildSuccessResponse(
                "USER_ACTIVITY_SIGNAL_RECORDED",
                "User activity signal recorded",
                data,
                request
        ));
    }

    @GetMapping("/me/state")
    public ResponseEntity<BaseResponse<List<UserActivityStateResponseDto>>> getMyState(
            Authentication authentication,
            HttpServletRequest request
    ) {
        User requester = getCurrentUser(authentication);
        return ResponseEntity.ok(buildSuccessResponse(
                "USER_ACTIVITY_STATE_FOUND",
                "User activity state found",
                passiveActivityService.getMyActivityStates(requester.getId()),
                request
        ));
    }

    @PatchMapping("/settings")
    public ResponseEntity<BaseResponse<PassiveActivitySettingsResponseDto>> updateSettings(
            @Valid @RequestBody PassiveActivitySettingsRequest body,
            Authentication authentication,
            HttpServletRequest request
    ) {
        User requester = getCurrentUser(authentication);
        return ResponseEntity.ok(buildSuccessResponse(
                "PASSIVE_ACTIVITY_SETTINGS_UPDATED",
                "Passive activity settings updated",
                passiveActivityService.updateSettings(requester.getId(), body.passiveActivityAssistEnabled()),
                request
        ));
    }

    private <T> BaseResponse<T> buildSuccessResponse(
            String code,
            String message,
            T data,
            HttpServletRequest request
    ) {
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
            throw new ForbiddenException("Authentication required");
        }

        String email = authentication.getName();
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}
