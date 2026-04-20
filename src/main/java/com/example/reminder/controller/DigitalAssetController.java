package com.example.reminder.controller;

import com.example.reminder.domain.model.DigitalAssetModel;
import com.example.reminder.domain.model.DecryptTokenModel;
import com.example.reminder.domain.model.DecryptedDigitalAssetModel;
import com.example.reminder.dto.digitalasset.CreateDigitalAssetCommand;
import com.example.reminder.dto.digitalasset.CreateDigitalAssetRequest;
import com.example.reminder.dto.digitalasset.ConsumeSecretTokenCommand;
import com.example.reminder.dto.digitalasset.ConsumeSecretTokenRequest;
import com.example.reminder.dto.digitalasset.ConsumeSecretTokenResponseDto;
import com.example.reminder.dto.digitalasset.DecryptDigitalAssetCommand;
import com.example.reminder.dto.digitalasset.DecryptDigitalAssetRequest;
import com.example.reminder.dto.digitalasset.DecryptDigitalAssetResponseDto;
import com.example.reminder.dto.digitalasset.DigitalAssetResponseDto;
import com.example.reminder.exception.BadRequestException;
import com.example.reminder.service.DigitalAssetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/digital-assets")
@RequiredArgsConstructor
public class DigitalAssetController {

    private final DigitalAssetService digitalAssetService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DigitalAssetResponseDto create(@Valid @RequestBody CreateDigitalAssetRequest request) {
        CreateDigitalAssetCommand command = new CreateDigitalAssetCommand(
                request.userId(),
                request.name(),
                request.type(),
                request.identifier(),
                request.secret(),
                request.instructions()
        );

        return toDto(digitalAssetService.create(command));
    }

    @PostMapping("/{assetId}/decrypt")
    public DecryptDigitalAssetResponseDto decrypt(
            @PathVariable Long assetId,
            @Valid @RequestBody DecryptDigitalAssetRequest request,
            HttpServletRequest httpServletRequest
    ) {
        String actorId = resolveActorId(httpServletRequest);

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
        public ConsumeSecretTokenResponseDto consumeSecretToken(
            @PathVariable String token,
            @Valid @RequestBody ConsumeSecretTokenRequest request,
            HttpServletRequest httpServletRequest
        ) {
        String actorId = resolveActorId(httpServletRequest);

        ConsumeSecretTokenCommand command = new ConsumeSecretTokenCommand(
            token,
            actorId,
            httpServletRequest.getRemoteAddr(),
            resolveRequestId(httpServletRequest),
            httpServletRequest.getHeader("User-Agent"),
            httpServletRequest.getRequestURI(),
            httpServletRequest.getMethod()
        );

        return toConsumeDto(digitalAssetService.consumeSecretToken(command));
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

    private String resolveActorId(HttpServletRequest request) {
        String actorId = request.getHeader("X-Actor-Id");
        if (actorId == null || actorId.isBlank()) {
            throw new BadRequestException("Missing X-Actor-Id header");
        }

        return actorId;
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }

        return requestId;
    }
}
