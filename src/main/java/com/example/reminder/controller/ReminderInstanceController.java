package com.example.reminder.controller;

import com.example.reminder.dto.common.BaseResponse;
import com.example.reminder.dto.common.PagedResponseDto;
import com.example.reminder.dto.reminderinstance.ReminderInstanceResponseDto;
import com.example.reminder.dto.reminderinstance.TodayReminderScheduleDto;
import com.example.reminder.entity.User;
import com.example.reminder.exception.ForbiddenException;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.ReminderInstanceService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderInstanceController {

    private final ReminderInstanceService reminderInstanceService;
    private final UserRepository userRepository;

        @GetMapping("/instances/today")
        public ResponseEntity<BaseResponse<List<TodayReminderScheduleDto>>> getTodaySchedules(
                        Authentication authentication,
                        HttpServletRequest request
        ) {
                User requester = getCurrentUser(authentication);
                List<TodayReminderScheduleDto> data = reminderInstanceService.getTodaySchedules(requester.getId());

                return ResponseEntity.ok(buildSuccessResponse(
                                "TODAY_REMINDER_SCHEDULE_LIST_FOUND",
                                "Today's reminder schedules retrieved successfully",
                                data,
                                request
                ));
        }

    @GetMapping("/{reminderId}/instances")
    public ResponseEntity<BaseResponse<PagedResponseDto<ReminderInstanceResponseDto>>> getInstances(
            @PathVariable Long reminderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication,
            HttpServletRequest request
    ) {
        User requester = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "scheduledTime"));
        PagedResponseDto<ReminderInstanceResponseDto> data = PagedResponseDto.from(
                reminderInstanceService.getByReminderId(reminderId, requester.getId(), pageable)
        );

        return ResponseEntity.ok(buildSuccessResponse(
                "REMINDER_INSTANCE_LIST_FOUND",
                "Reminder instances retrieved successfully",
                data,
                request
        ));
    }

    @GetMapping("/{reminderId}/instances/{instanceId}")
    public ResponseEntity<BaseResponse<ReminderInstanceResponseDto>> getInstance(
            @PathVariable Long reminderId,
            @PathVariable Long instanceId,
            Authentication authentication,
            HttpServletRequest request
    ) {
        User requester = getCurrentUser(authentication);
        return ResponseEntity.ok(buildSuccessResponse(
                "REMINDER_INSTANCE_FOUND",
                "Reminder instance retrieved successfully",
                reminderInstanceService.getById(reminderId, instanceId, requester.getId()),
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
            throw new ForbiddenException("User must be authenticated");
        }

        String email = authentication.getName();
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}