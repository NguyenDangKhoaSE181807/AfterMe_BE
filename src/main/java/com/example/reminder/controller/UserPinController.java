package com.example.reminder.controller;

import com.example.reminder.dto.securitypin.SetUserPinRequest;
import com.example.reminder.dto.securitypin.UserPinStatusResponse;
import com.example.reminder.dto.securitypin.UserPinVerifiedResponse;
import com.example.reminder.dto.securitypin.VerifyUserPinRequest;
import com.example.reminder.service.UserPinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/security-pin")
@RequiredArgsConstructor
public class UserPinController {

    private final UserPinService userPinService;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPin(@Valid @RequestBody SetUserPinRequest request) {
        userPinService.setPin(resolveCurrentUserId(), request.pin());
    }

    @PostMapping("/verify")
    public UserPinVerifiedResponse verify(@Valid @RequestBody VerifyUserPinRequest request) {
        userPinService.verifyPin(resolveCurrentUserId(), request.pin());
        return new UserPinVerifiedResponse(true);
    }

    @GetMapping("/status")
    public UserPinStatusResponse status() {
        return userPinService.getStatus(resolveCurrentUserId());
    }

    private Long resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            throw new org.springframework.security.access.AccessDeniedException("User is not authenticated");
        }

        Jwt jwt = jwtAuthenticationToken.getToken();
        Object uidClaim = jwt.getClaims().get("uid");
        if (uidClaim instanceof Number number) {
            return number.longValue();
        }
        if (uidClaim instanceof String uidString && !uidString.isBlank()) {
            return Long.parseLong(uidString);
        }

        throw new org.springframework.security.access.AccessDeniedException("User id claim is missing");
    }
}
