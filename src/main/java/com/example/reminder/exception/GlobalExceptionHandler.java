package com.example.reminder.exception;

import com.example.reminder.dto.common.BaseResponse;
import com.example.reminder.dto.common.ErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private BaseResponse<Void> buildErrorResponse(
            String code,
            String message,
            List<ErrorDetail> errors,
            HttpServletRequest request
    ) {
        return BaseResponse.<Void>builder()
                .success(false)
                .code(code)
                .message(message)
                .data(null)
                .errors(errors)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .requestId(request.getHeader("X-Request-Id"))
                .build();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse(
                        "NOT_FOUND",
                        ex.getMessage(),
                        List.of(new ErrorDetail("NOT_FOUND", ex.getMessage(), null)),
                        request
                ));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<BaseResponse<Void>> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(
                        "BAD_REQUEST",
                        ex.getMessage(),
                        List.of(new ErrorDetail("BAD_REQUEST", ex.getMessage(), null)),
                        request
                ));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<BaseResponse<Void>> handleForbidden(
            ForbiddenException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(buildErrorResponse(
                        "FORBIDDEN",
                        ex.getMessage(),
                        List.of(new ErrorDetail("FORBIDDEN", ex.getMessage(), null)),
                        request
                ));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<BaseResponse<Void>> handleTooManyRequests(
            TooManyRequestsException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(buildErrorResponse(
                        "TOO_MANY_REQUESTS",
                        ex.getMessage(),
                        List.of(new ErrorDetail("TOO_MANY_REQUESTS", ex.getMessage(), null)),
                        request
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        String firstError = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Invalid request");

        List<ErrorDetail> errors = fieldErrors.entrySet().stream()
                .map(entry -> new ErrorDetail("VALIDATION_ERROR", entry.getValue(), entry.getKey()))
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(
                        "VALIDATION_ERROR",
                        firstError,
                        errors,
                        request
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse(
                        "INTERNAL_SERVER_ERROR",
                        "An unexpected error occurred",
                        List.of(new ErrorDetail("INTERNAL_SERVER_ERROR", ex.getMessage(), null)),
                        request
                ));
    }
}





