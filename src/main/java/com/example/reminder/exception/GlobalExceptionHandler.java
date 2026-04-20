package com.example.reminder.exception;

import com.example.reminder.exception.BadRequestException;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.exception.TooManyRequestsException;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse("NOT_FOUND", ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse("BAD_REQUEST", ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiErrorResponse> handleTooManyRequests(TooManyRequestsException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ApiErrorResponse("TOO_MANY_REQUESTS", ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Invalid request");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse("VALIDATION_ERROR", message, LocalDateTime.now()));
    }
}





