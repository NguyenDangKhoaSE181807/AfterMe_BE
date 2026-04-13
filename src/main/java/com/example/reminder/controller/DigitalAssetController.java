package com.example.reminder.controller;

import com.example.reminder.domain.model.DigitalAssetModel;
import com.example.reminder.dto.digitalasset.CreateDigitalAssetCommand;
import com.example.reminder.dto.digitalasset.CreateDigitalAssetRequest;
import com.example.reminder.dto.digitalasset.DigitalAssetResponseDto;
import com.example.reminder.service.DigitalAssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
}
