package com.example.reminder.controller;

import com.example.reminder.dto.common.BaseResponse;
import com.example.reminder.dto.trustedcontact.CreateTrustedContactRequest;
import com.example.reminder.dto.trustedcontact.TrustedContactResponseDto;
import com.example.reminder.dto.trustedcontact.UpdateTrustedContactRequest;
import com.example.reminder.service.TrustedContactService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/trusted-contacts")
@RequiredArgsConstructor
public class TrustedContactController {

    private final TrustedContactService trustedContactService;

    @GetMapping
    public ResponseEntity<BaseResponse<List<TrustedContactResponseDto>>> getAll(HttpServletRequest request, Authentication authentication) {
        List<TrustedContactResponseDto> data = trustedContactService.getAllTrustedContacts(authentication);
        BaseResponse<List<TrustedContactResponseDto>> body = buildSuccessResponse(
                "GET_TRUSTED_CONTACTS_SUCCESS",
                "Get trusted contacts success",
                data,
                request
        );
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<TrustedContactResponseDto>> getById(@PathVariable Long id, HttpServletRequest request, Authentication authentication) {
        TrustedContactResponseDto data = trustedContactService.getTrustedContactById(id, authentication);
        BaseResponse<TrustedContactResponseDto> body = buildSuccessResponse(
                "GET_TRUSTED_CONTACT_SUCCESS",
                "Get trusted contact success",
                data,
                request
        );
        return ResponseEntity.ok(body);
    }

    @PostMapping
    public ResponseEntity<BaseResponse<TrustedContactResponseDto>> create(@Valid @RequestBody CreateTrustedContactRequest req, Authentication authentication, HttpServletRequest request) {
        TrustedContactResponseDto result = trustedContactService.createTrustedContact(req, authentication);
        BaseResponse<TrustedContactResponseDto> body = buildSuccessResponse(
                "CREATE_TRUSTED_CONTACT_SUCCESS",
                "Create trusted contact success",
                result,
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<TrustedContactResponseDto>> update(@PathVariable Long id, @Valid @RequestBody UpdateTrustedContactRequest req, Authentication authentication, HttpServletRequest request) {
        TrustedContactResponseDto result = trustedContactService.updateTrustedContact(id, req, authentication);
        BaseResponse<TrustedContactResponseDto> body = buildSuccessResponse(
                "UPDATE_TRUSTED_CONTACT_SUCCESS",
                "Update trusted contact success",
                result,
                request
        );
        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        trustedContactService.deleteTrustedContact(id, authentication);
        BaseResponse<Void> body = buildSuccessResponse(
                "DELETE_TRUSTED_CONTACT_SUCCESS",
                "Delete trusted contact success",
                null,
                request
        );
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<BaseResponse<TrustedContactResponseDto>> toggle(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        TrustedContactResponseDto result = trustedContactService.toggleTrustedContactStatus(id, authentication);
        BaseResponse<TrustedContactResponseDto> body = buildSuccessResponse(
                "TOGGLE_TRUSTED_CONTACT_SUCCESS",
                "Toggle trusted contact success",
                result,
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
