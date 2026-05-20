package com.example.reminder.controller;

import com.example.reminder.domain.model.DigitalAssetModel;
import com.example.reminder.domain.model.DecryptTokenModel;
import com.example.reminder.domain.model.DecryptedDigitalAssetModel;
import com.example.reminder.dto.common.PagedResponseDto;
import com.example.reminder.dto.digitalasset.AssetAuditContext;
import com.example.reminder.dto.digitalasset.CreateDigitalAssetCommand;
import com.example.reminder.dto.digitalasset.CreateDigitalAssetRequest;
import com.example.reminder.dto.digitalasset.ConsumeSecretTokenCommand;
import com.example.reminder.dto.digitalasset.ConsumeSecretTokenRequest;
import com.example.reminder.dto.digitalasset.ConsumeSecretTokenResponseDto;
import com.example.reminder.dto.digitalasset.DeleteDigitalAssetResponseDto;
import com.example.reminder.dto.digitalasset.DigitalAssetDetailResponseDto;
import com.example.reminder.dto.digitalasset.DigitalAssetListResponseDto;
import com.example.reminder.dto.digitalasset.DecryptDigitalAssetCommand;
import com.example.reminder.dto.digitalasset.DecryptDigitalAssetRequest;
import com.example.reminder.dto.digitalasset.DecryptDigitalAssetResponseDto;
import com.example.reminder.dto.digitalasset.DigitalAssetResponseDto;
import com.example.reminder.dto.digitalasset.UpdateDigitalAssetRequest;
import com.example.reminder.dto.digitalasset.UpdateDigitalAssetSecretRequest;
import com.example.reminder.dto.digitalasset.UpdateDigitalAssetSecretResponseDto;
import com.example.reminder.service.DigitalAssetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/digital-assets")
@RequiredArgsConstructor
public class DigitalAssetController {

    private final DigitalAssetService digitalAssetService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DigitalAssetResponseDto create(
            @Valid @RequestBody CreateDigitalAssetRequest request,
            HttpServletRequest httpServletRequest,
            Authentication authentication
    ) {
        Long currentUserId = resolveCurrentUserId();
        AssetAuditContext auditContext = buildAuditContext(authentication, httpServletRequest);
        CreateDigitalAssetCommand command = new CreateDigitalAssetCommand(
                currentUserId,
                request.name(),
                request.type(),
                request.identifier(),
                request.secret(),
                request.instructions()
        );

        return toDto(digitalAssetService.create(command, auditContext));
    }

    @GetMapping
    public PagedResponseDto<DigitalAssetListResponseDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search
    ) {
        Long currentUserId = resolveCurrentUserId();
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<DigitalAssetListResponseDto> assets = digitalAssetService.getAssets(currentUserId, search, pageable);
        return PagedResponseDto.from(assets);
    }

    @GetMapping("/{assetId}")
    public DigitalAssetDetailResponseDto getById(@PathVariable Long assetId) {
        Long currentUserId = resolveCurrentUserId();
        return digitalAssetService.getAsset(currentUserId, assetId);
    }

    @PutMapping("/{assetId}")
    public DigitalAssetDetailResponseDto update(
            @PathVariable Long assetId,
            @Valid @RequestBody UpdateDigitalAssetRequest request,
            HttpServletRequest httpServletRequest,
            Authentication authentication
    ) {
        Long currentUserId = resolveCurrentUserId();
        AssetAuditContext auditContext = buildAuditContext(authentication, httpServletRequest);
        return digitalAssetService.updateAsset(currentUserId, assetId, request, auditContext);
    }

    @PutMapping("/{assetId}/secret")
    public UpdateDigitalAssetSecretResponseDto updateSecret(
            @PathVariable Long assetId,
            @Valid @RequestBody UpdateDigitalAssetSecretRequest request,
            HttpServletRequest httpServletRequest,
            Authentication authentication
    ) {
        Long currentUserId = resolveCurrentUserId();
        AssetAuditContext auditContext = buildAuditContext(authentication, httpServletRequest);
        DigitalAssetDetailResponseDto updated = digitalAssetService.updateSecret(
                currentUserId,
                assetId,
                request.secret(),
                auditContext
        );

        return new UpdateDigitalAssetSecretResponseDto(
                "Secret updated successfully",
                updated.updatedAt()
        );
    }

    @DeleteMapping("/{assetId}")
    public DeleteDigitalAssetResponseDto delete(
            @PathVariable Long assetId,
            HttpServletRequest httpServletRequest,
            Authentication authentication
    ) {
        Long currentUserId = resolveCurrentUserId();
        AssetAuditContext auditContext = buildAuditContext(authentication, httpServletRequest);
        digitalAssetService.deleteAsset(currentUserId, assetId, auditContext);
        return new DeleteDigitalAssetResponseDto("Digital asset deleted successfully");
    }

    @PostMapping("/{assetId}/decrypt")
    public DecryptDigitalAssetResponseDto decrypt(
            @PathVariable Long assetId,
            @Valid @RequestBody DecryptDigitalAssetRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest
    ) {
        String actorId = resolveActorId(authentication);

        DecryptDigitalAssetCommand command = new DecryptDigitalAssetCommand(
                assetId,
                request.trustedContactId(),
                actorId,
                httpServletRequest.getRemoteAddr(),
                resolveRequestId(httpServletRequest),
                httpServletRequest.getHeader("User-Agent"),
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()
        );

        return toDecryptTokenDto(digitalAssetService.decrypt(command));
    }

    @PostMapping("/secrets/{token}/consume")
    public ResponseEntity<ConsumeSecretTokenResponseDto> consumeSecretToken(
            @PathVariable String token,
            @Valid @RequestBody ConsumeSecretTokenRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest
    ) {
        String actorId = resolveActorId(authentication);

        ConsumeSecretTokenCommand command = new ConsumeSecretTokenCommand(
                token,
                actorId,
                httpServletRequest.getRemoteAddr(),
                resolveRequestId(httpServletRequest),
                httpServletRequest.getHeader("User-Agent"),
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()
        );

        ConsumeSecretTokenResponseDto response = toConsumeDto(digitalAssetService.consumeSecretToken(command));
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(response);
    }

    private DigitalAssetResponseDto toDto(DigitalAssetModel model) {
        return new DigitalAssetResponseDto(
                model.id(),
                model.userId(),
                model.name(),
                model.type(),
                model.identifier(),
                model.identifierType(),
                model.identifierValue(),
                model.accessInstructions(),
                model.isActive(),
                model.createdAt()
        );
    }

    private DecryptDigitalAssetResponseDto toDecryptTokenDto(DecryptTokenModel model) {
        return new DecryptDigitalAssetResponseDto(
                model.assetId(),
                model.oneTimeToken(),
                model.expiresAt()
        );
    }

    private ConsumeSecretTokenResponseDto toConsumeDto(DecryptedDigitalAssetModel model) {
        return new ConsumeSecretTokenResponseDto(
                model.assetId(),
                model.secret(),
                model.decryptedAt()
        );
    }

    private String resolveActorId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new org.springframework.security.access.AccessDeniedException("Missing authenticated actor");
        }

        return authentication.getName();
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }

        return requestId;
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

    private AssetAuditContext buildAuditContext(Authentication authentication, HttpServletRequest request) {
        return new AssetAuditContext(
                resolveActorId(authentication),
                request.getRemoteAddr(),
                resolveRequestId(request),
                request.getHeader("User-Agent"),
                request.getRequestURI(),
                request.getMethod()
        );
    }
}
