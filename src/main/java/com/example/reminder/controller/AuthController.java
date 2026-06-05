package com.example.reminder.controller;

import com.example.reminder.dto.auth.CookieAuthResponseDto;
import com.example.reminder.dto.auth.AuthResponseDto;
import com.example.reminder.dto.auth.ChangePasswordWithCodeRequest;
import com.example.reminder.dto.auth.ResendVerificationRequest;
import com.example.reminder.dto.auth.SignUpPendingResponseDto;
import com.example.reminder.dto.auth.UserSessionResponseDto;
import com.example.reminder.dto.common.BaseResponse;
import com.example.reminder.dto.auth.SignInRequest;
import com.example.reminder.dto.auth.SignUpRequest;
import com.example.reminder.dto.auth.VerifyEmailRequest;
import com.example.reminder.service.AuthService;
import com.example.reminder.exception.BadRequestException;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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

    private void setAccessTokenCookie(HttpServletResponse response, String accessToken, long ttlSeconds) {
        String cookieValue = String.format(
                "accessToken=%s; Path=/; Max-Age=%d; SameSite=Lax",
                accessToken,
                ttlSeconds
        );
        response.addHeader("Set-Cookie", cookieValue);
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken, long ttlSeconds) {
        String cookieValue = String.format(
                "refreshToken=%s; Path=/; Max-Age=%d; HttpOnly; SameSite=Lax",
                refreshToken,
                ttlSeconds
        );
        response.addHeader("Set-Cookie", cookieValue);
    }

    private void clearCookies(HttpServletResponse response) {
        String accessTokenCookie = "accessToken=; Path=/; Max-Age=0; SameSite=Lax";
        String refreshTokenCookie = "refreshToken=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax";
        response.addHeader("Set-Cookie", accessTokenCookie);
        response.addHeader("Set-Cookie", refreshTokenCookie);
    }

    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            throw new BadRequestException(cookieName + " cookie not found");
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new BadRequestException(cookieName + " cookie not found"));
    }

    private String resolveDeviceId(HttpServletRequest request) {
        String headerDeviceId = request.getHeader("X-Device-Id");
        if (headerDeviceId != null && !headerDeviceId.isBlank()) {
                return headerDeviceId.trim();
        }

        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
                throw new BadRequestException("X-Device-Id header is required");
        }

        return "ua-" + shortHash(userAgent);
    }

        private String resolveClientIp(HttpServletRequest request) {
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                        return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
        }

        private String shortHash(String source) {
                try {
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
                        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 32);
                } catch (NoSuchAlgorithmException ex) {
                        throw new IllegalStateException("SHA-256 not available", ex);
                }
        }

        private Long extractUserId(Authentication authentication) {
                if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
                        throw new BadRequestException("User is not authenticated");
                }

                Jwt jwt = jwtAuthenticationToken.getToken();
                Object uidClaim = jwt.getClaims().get("uid");
                if (uidClaim instanceof Number number) {
                        return number.longValue();
                }
                if (uidClaim instanceof String uidString && !uidString.isBlank()) {
                        return Long.parseLong(uidString);
                }

                throw new BadRequestException("User id claim is missing");
        }

    @PostMapping("/sign-up")
    public ResponseEntity<BaseResponse<SignUpPendingResponseDto>> signUp(
            @Valid @RequestBody SignUpRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = authService.registerUserForEmailVerification(
                request.email(),
                request.password(),
                request.fullName(),
                request.tonePreference(),
                request.dailyCheckInTime()
        );

        SignUpPendingResponseDto data = new SignUpPendingResponseDto(
                userId,
                request.email(),
                "Verification code sent to your email. Please verify to activate account"
        );

        BaseResponse<SignUpPendingResponseDto> body = buildSuccessResponse(
                "AUTH_SIGN_UP_PENDING_VERIFICATION",
                "Please verify your email to complete sign up",
                data,
                httpRequest
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<BaseResponse<SignUpPendingResponseDto>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = authService.verifyEmailAndActivateUser(request.userId(), request.code());

        SignUpPendingResponseDto data = new SignUpPendingResponseDto(
                userId,
                null,
                "Email verified successfully. Your account is now active"
        );

        BaseResponse<SignUpPendingResponseDto> body = buildSuccessResponse(
                "AUTH_EMAIL_VERIFIED",
                "Email verified successfully",
                data,
                httpRequest
        );

        return ResponseEntity.ok(body);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<BaseResponse<Void>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request,
            HttpServletRequest httpRequest
    ) {
        authService.resendVerificationCode(request.userId());

        BaseResponse<Void> body = buildSuccessResponse(
                "AUTH_VERIFICATION_CODE_RESENT",
                "Verification code has been resent",
                null,
                httpRequest
        );

        return ResponseEntity.ok(body);
    }

    @PostMapping("/password/change/request-code")
    public ResponseEntity<BaseResponse<Void>> requestPasswordChangeCode(
                        Authentication authentication,
            HttpServletRequest httpRequest
    ) {
                authService.sendPasswordChangeCode(authentication.getName());

        BaseResponse<Void> body = buildSuccessResponse(
                "AUTH_PASSWORD_CHANGE_CODE_SENT",
                "Password change verification code has been sent",
                null,
                httpRequest
        );

        return ResponseEntity.ok(body);
    }

    @PostMapping("/password/change/confirm")
    public ResponseEntity<BaseResponse<Void>> changePasswordWithCode(
            @Valid @RequestBody ChangePasswordWithCodeRequest request,
                        Authentication authentication,
            HttpServletRequest httpRequest
    ) {
                authService.changePasswordWithCode(authentication.getName(), request.code(), request.newPassword());

        BaseResponse<Void> body = buildSuccessResponse(
                "AUTH_PASSWORD_CHANGED",
                "Password changed successfully",
                null,
                httpRequest
        );

        return ResponseEntity.ok(body);
    }

    @PostMapping("/sign-in")
    public ResponseEntity<BaseResponse<CookieAuthResponseDto>> signIn(
            @Valid @RequestBody SignInRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        String deviceId = resolveDeviceId(httpRequest);
        String ipAddress = resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        AuthResponseDto authResponse = authService.signIn(request.email(), request.password(), deviceId, ipAddress, userAgent);
        
        setAccessTokenCookie(response, authResponse.accessToken(), authResponse.accessTokenExpiresInSeconds());
        setRefreshTokenCookie(response, authResponse.refreshToken(), 1209600); // 14 days
        
        CookieAuthResponseDto data = new CookieAuthResponseDto(
                authResponse.userId(),
                authResponse.email(),
                authResponse.fullName(),
                authResponse.role(),
                authResponse.accessToken(),
                "Sign in successful"
        );

        BaseResponse<CookieAuthResponseDto> body = buildSuccessResponse(
                "AUTH_SIGN_IN_SUCCESS",
                "Sign in successful",
                data,
                httpRequest
        );

        return ResponseEntity.ok(body);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<BaseResponse<CookieAuthResponseDto>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = extractCookieValue(request, "refreshToken");
        String deviceId = resolveDeviceId(request);
        String ipAddress = resolveClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        AuthResponseDto authResponse = authService.refreshToken(refreshToken, deviceId, ipAddress, userAgent);
        
        setAccessTokenCookie(response, authResponse.accessToken(), authResponse.accessTokenExpiresInSeconds());
        setRefreshTokenCookie(response, authResponse.refreshToken(), 1209600); // 14 days
        
        CookieAuthResponseDto data = new CookieAuthResponseDto(
                authResponse.userId(),
                authResponse.email(),
                authResponse.fullName(),
                authResponse.role(),
                authResponse.accessToken(),
                "Token refreshed"
        );

        BaseResponse<CookieAuthResponseDto> body = buildSuccessResponse(
                "AUTH_REFRESH_SUCCESS",
                "Token refreshed",
                data,
                request
        );

        return ResponseEntity.ok(body);
    }

    @PostMapping("/log-out")
        public ResponseEntity<BaseResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = extractCookieValue(request, "refreshToken");
                String deviceId = resolveDeviceId(request);
                authService.logout(refreshToken, deviceId);
        clearCookies(response);

                BaseResponse<Void> body = buildSuccessResponse(
                                "AUTH_LOGOUT_SUCCESS",
                                "Logged out successfully",
                                null,
                                request
                );

                return ResponseEntity.ok(body);
    }

    @GetMapping("/sessions")
    public ResponseEntity<BaseResponse<List<UserSessionResponseDto>>> listActiveSessions(
                        Authentication authentication,
            HttpServletRequest request
    ) {
                Long userId = extractUserId(authentication);
        String deviceId = resolveDeviceId(request);
        List<UserSessionResponseDto> sessions = authService.listActiveSessionsByUserId(userId, deviceId);

        BaseResponse<List<UserSessionResponseDto>> body = buildSuccessResponse(
                "AUTH_LIST_SESSIONS_SUCCESS",
                "Active sessions retrieved",
                sessions,
                request
        );

        return ResponseEntity.ok(body);
    }
}
