package com.example.reminder.controller;

import com.example.reminder.dto.plan.CreatePlanRequest;
import com.example.reminder.dto.plan.PlanResponseDto;
import com.example.reminder.dto.plan.UpdatePlanRequest;
import com.example.reminder.dto.common.BaseResponse;
import com.example.reminder.dto.plan.CreatePlanCommand;
import com.example.reminder.dto.plan.UpdatePlanCommand;
import com.example.reminder.service.PlanService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping
    public ResponseEntity<BaseResponse<List<PlanResponseDto>>> findAllActive(
            HttpServletRequest request
    ) {
        List<PlanResponseDto> plansData = planService.findAllActive();

        BaseResponse<List<PlanResponseDto>> body = buildSuccessResponse(
                "GET_PLANS_SUCCESS",
                "Get Plans list Success",
                plansData,
                request
        );

        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<PlanResponseDto>> findById(@PathVariable Long id, HttpServletRequest request) {
        PlanResponseDto data = planService.findById(id);

        BaseResponse<PlanResponseDto> body = buildSuccessResponse(
                "GET_PLAN_SUCCESS",
                "Get Plan Success",
                data,
                request
        );

        return ResponseEntity.ok(body);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<PlanResponseDto>> create(
            @Valid @RequestBody CreatePlanRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        CreatePlanCommand command = new CreatePlanCommand(
                request.name(),
                request.price(),
                request.billingCycle(),
                request.maxReminders(),
                request.maxTrustedContacts(),
                request.maxDigitalAssets(),
                request.features(),
                request.isActive()
        );

        PlanResponseDto result = planService.create(authentication, command);

        BaseResponse<PlanResponseDto> body = buildSuccessResponse(
                "CREATE_PLAN_SUCCESS",
                "Create Plan Success",
                result,
                httpRequest
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<PlanResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePlanRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        UpdatePlanCommand command = new UpdatePlanCommand(
                request.name(),
                request.price(),
                request.billingCycle(),
                request.maxReminders(),
                request.maxTrustedContacts(),
                request.maxDigitalAssets(),
                request.features(),
                request.isActive()
        );

        PlanResponseDto result = planService.update(authentication, id, command);

        BaseResponse<PlanResponseDto> body = buildSuccessResponse(
            "UPDATE_PLAN_SUCCESS",
            "Update Plan Success",
            result,
            httpRequest
        );

        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> delete(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request
    ) {
        planService.deleteById(authentication, id);

        BaseResponse<Void> body = buildSuccessResponse(
                "DELETE_PLAN_SUCCESS",
                "Delete Plan Success",
                null,
                request
        );

        return ResponseEntity.ok(body);
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
}
