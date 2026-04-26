package com.example.reminder.controller;

import com.example.reminder.domain.enums.ReminderStatus;
import com.example.reminder.domain.enums.ScheduleType;
import com.example.reminder.domain.enums.UserRole;
import com.example.reminder.dto.reminder.CreateReminderRequest;
import com.example.reminder.dto.reminder.CreateReminderScheduleRequest;
import com.example.reminder.dto.reminder.AdminReminderMetadataDto;
import com.example.reminder.dto.reminder.AdminReminderOverviewDto;
import com.example.reminder.dto.reminder.ReminderResponseDto;
import com.example.reminder.dto.reminder.ReminderScheduleResponseDto;
import com.example.reminder.dto.reminder.UpdateReminderRequest;
import com.example.reminder.dto.reminder.UpdateReminderScheduleRequest;
import com.example.reminder.dto.reminder.CreateReminderCommand;
import com.example.reminder.dto.reminder.UpdateReminderCommand;
import com.example.reminder.dto.common.BaseResponse;
import com.example.reminder.domain.model.ReminderModel;
import com.example.reminder.entity.User;
import com.example.reminder.exception.ForbiddenException;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.repository.ReminderScheduleRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.ReminderService;
import com.example.reminder.service.ReminderScheduleService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;
    private final ReminderScheduleService reminderScheduleService;
    private final ReminderScheduleRepository reminderScheduleRepository;
    private final UserRepository userRepository;

    // ==================== Reminder APIs ====================

    @GetMapping
    public ResponseEntity<BaseResponse<?>> findAll(
            @RequestParam(required = false) Long userId,
            Authentication authentication,
            HttpServletRequest request) {
        User requester = getCurrentUser(authentication);

        if (userId != null) {
            if (requester.getRole() != UserRole.ADMIN) {
                throw new ForbiddenException("No permission to query this API");
            }
            return ResponseEntity.ok(buildSuccessResponse(
                    "REMINDER_ADMIN_OVERVIEW_FOUND",
                    "Reminder metadata retrieved",
                    toAdminOverview(userId),
                    request
            ));
        }

        List<ReminderResponseDto> ownReminders = reminderService.findAll(requester.getId()).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(buildSuccessResponse(
                "REMINDER_LIST_FOUND",
                "Reminder list retrieved",
                ownReminders,
                request
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<ReminderResponseDto>> findById(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request) {
        User requester = getCurrentUser(authentication);
        ReminderModel reminder = reminderService.findById(id);
        if (!reminder.userId().equals(requester.getId())) {
            throw new ForbiddenException("No permission to view this reminder");
        }
        return ResponseEntity.ok(buildSuccessResponse(
                "REMINDER_FOUND",
                "Reminder retrieved successfully",
                toDto(reminder),
                request
        ));
    }

    @PostMapping
    public ResponseEntity<BaseResponse<ReminderResponseDto>> create(
            @Valid @RequestBody CreateReminderRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        User requester = getCurrentUser(authentication);
        
        CreateReminderCommand command = new CreateReminderCommand(
                requester.getId(),
                request.habitId(),
                request.title(),
                request.description(),
                request.tone(),
                request.safetyEnabled()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(buildSuccessResponse(
                "REMINDER_CREATED",
                "Reminder created successfully",
                toDto(reminderService.create(command)),
                httpRequest
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<ReminderResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReminderRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        User requester = getCurrentUser(authentication);
        ReminderModel existing = reminderService.findById(id);
        if (!existing.userId().equals(requester.getId())) {
            throw new ForbiddenException("No permission to update this reminder");
        }
        
        UpdateReminderCommand command = new UpdateReminderCommand(
                requester.getId(),
                request.habitId(),
                request.title(),
                request.description(),
                request.tone(),
                request.safetyEnabled()
        );

        return ResponseEntity.ok(buildSuccessResponse(
                "REMINDER_UPDATED",
                "Reminder updated successfully",
                toDto(reminderService.update(id, command)),
                httpRequest
        ));
    }

    @PatchMapping("/{id}/pause")
    public ResponseEntity<BaseResponse<ReminderResponseDto>> pause(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request) {
        User requester = getCurrentUser(authentication);
        ReminderModel existing = reminderService.findById(id);
        if (!existing.userId().equals(requester.getId())) {
            throw new ForbiddenException("No permission to pause this reminder");
        }
        return ResponseEntity.ok(buildSuccessResponse(
                "REMINDER_PAUSED",
                "Reminder paused successfully",
                toDto(reminderService.pause(id)),
                request
        ));
    }

    @PatchMapping("/{id}/resume")
    public ResponseEntity<BaseResponse<ReminderResponseDto>> resume(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request) {
        User requester = getCurrentUser(authentication);
        ReminderModel existing = reminderService.findById(id);
        if (!existing.userId().equals(requester.getId())) {
            throw new ForbiddenException("No permission to resume this reminder");
        }
        return ResponseEntity.ok(buildSuccessResponse(
                "REMINDER_RESUMED",
                "Reminder resumed successfully",
                toDto(reminderService.resume(id)),
                request
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> archive(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request) {
        User requester = getCurrentUser(authentication);
        ReminderModel existing = reminderService.findById(id);
        if (!existing.userId().equals(requester.getId())) {
            throw new ForbiddenException("No permission to delete this reminder");
        }
        reminderService.archive(id);
        return ResponseEntity.ok(buildSuccessResponse(
                "REMINDER_ARCHIVED",
                "Reminder archived successfully",
                null,
                request
        ));
    }

    // ==================== ReminderSchedule APIs ====================

    @PostMapping("/{reminderId}/schedules")
    public ResponseEntity<BaseResponse<ReminderScheduleResponseDto>> createSchedule(
            @PathVariable Long reminderId,
            @Valid @RequestBody CreateReminderScheduleRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        User requester = getCurrentUser(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(buildSuccessResponse(
                "REMINDER_SCHEDULE_CREATED",
                "Reminder schedule created successfully",
                reminderScheduleService.create(reminderId, requester.getId(), request),
                httpRequest
        ));
    }

    @GetMapping("/{reminderId}/schedules")
    public ResponseEntity<BaseResponse<List<ReminderScheduleResponseDto>>> getSchedules(
            @PathVariable Long reminderId,
            Authentication authentication,
            HttpServletRequest request) {
        User requester = getCurrentUser(authentication);
        return ResponseEntity.ok(buildSuccessResponse(
                "REMINDER_SCHEDULE_LIST_FOUND",
                "Reminder schedules retrieved successfully",
                reminderScheduleService.getByReminderId(reminderId, requester.getId()),
                request
        ));
    }

    @GetMapping("/{reminderId}/schedules/{scheduleId}")
    public ResponseEntity<BaseResponse<ReminderScheduleResponseDto>> getSchedule(
            @PathVariable Long reminderId,
            @PathVariable Long scheduleId,
            Authentication authentication,
            HttpServletRequest request) {
        User requester = getCurrentUser(authentication);
        return ResponseEntity.ok(buildSuccessResponse(
                "REMINDER_SCHEDULE_FOUND",
                "Reminder schedule retrieved successfully",
                reminderScheduleService.getById(reminderId, scheduleId, requester.getId()),
                request
        ));
    }

    @PutMapping("/{reminderId}/schedules/{scheduleId}")
    public ResponseEntity<BaseResponse<ReminderScheduleResponseDto>> updateSchedule(
            @PathVariable Long reminderId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody UpdateReminderScheduleRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        User requester = getCurrentUser(authentication);
        return ResponseEntity.ok(buildSuccessResponse(
                "REMINDER_SCHEDULE_UPDATED",
                "Reminder schedule updated successfully",
                reminderScheduleService.update(reminderId, scheduleId, requester.getId(), request),
                httpRequest
        ));
    }

    @DeleteMapping("/{reminderId}/schedules/{scheduleId}")
    public ResponseEntity<BaseResponse<Void>> deleteSchedule(
            @PathVariable Long reminderId,
            @PathVariable Long scheduleId,
            Authentication authentication,
            HttpServletRequest request) {
        User requester = getCurrentUser(authentication);
        reminderScheduleService.delete(reminderId, scheduleId, requester.getId());
        return ResponseEntity.ok(buildSuccessResponse(
                "REMINDER_SCHEDULE_DELETED",
                "Reminder schedule deleted successfully",
                null,
                request
        ));
    }

    // ==================== Helper Methods ====================

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

    private AdminReminderOverviewDto toAdminOverview(Long targetUserId) {
        List<ReminderModel> reminders = reminderService.findAll(targetUserId);
        Map<ReminderStatus, Long> statusCounts = reminders.stream()
            .collect(Collectors.groupingBy(ReminderModel::status, Collectors.counting()));

        List<AdminReminderMetadataDto> reminderMetadata = reminders.stream()
            .map(this::toAdminMetadata)
            .toList();

        return new AdminReminderOverviewDto(
            targetUserId,
            reminders.size(),
            statusCounts,
            reminderMetadata
        );
    }

    private AdminReminderMetadataDto toAdminMetadata(ReminderModel reminder) {
        List<ReminderScheduleResponseDto> schedules = reminderScheduleRepository
            .findByReminderIdAndDeletedAtIsNull(reminder.id())
            .stream()
            .map(schedule -> new ReminderScheduleResponseDto(
                schedule.getId(),
                schedule.getReminder().getId(),
                schedule.getType(),
                schedule.getIntervalValue(),
                schedule.getDaysOfWeek(),
                schedule.getStartDatetime(),
                schedule.getEndDatetime()
            ))
            .toList();

        Set<ScheduleType> frequencyTypes = schedules.stream()
            .map(ReminderScheduleResponseDto::type)
            .collect(Collectors.toSet());

        List<Integer> intervalValues = schedules.stream()
            .map(ReminderScheduleResponseDto::intervalValue)
            .filter(value -> value != null)
            .toList();

        return new AdminReminderMetadataDto(
            reminder.id(),
            reminder.status(),
            schedules.size(),
            frequencyTypes,
            intervalValues,
            reminder.createdAt(),
            reminder.updatedAt(),
            reminder.deletedAt()
        );
    }

    private ReminderResponseDto toDto(ReminderModel reminder) {
        return new ReminderResponseDto(
                reminder.id(),
                reminder.userId(),
                reminder.habitId(),
                reminder.title(),
                reminder.description(),
                reminder.tone(),
                reminder.safetyEnabled(),
                reminder.status(),
                reminder.createdAt(),
                reminder.updatedAt()
        );
    }
}





