package com.example.reminder.controller;

import com.example.reminder.dto.trustedcontact.CreateTrustedContactRequest;
import com.example.reminder.dto.trustedcontact.TrustedContactResponseDto;
import com.example.reminder.dto.trustedcontact.UpdateTrustedContactRequest;
import com.example.reminder.service.TrustedContactService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trusted-contacts")
@RequiredArgsConstructor
public class TrustedContactController {

    private final TrustedContactService trustedContactService;

    @GetMapping
    public List<TrustedContactResponseDto> list(Authentication authentication) {
        return trustedContactService.getAllTrustedContacts(authentication);
    }

    @GetMapping("/{contactId}")
    public TrustedContactResponseDto getById(@PathVariable Long contactId, Authentication authentication) {
        return trustedContactService.getTrustedContactById(contactId, authentication);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrustedContactResponseDto create(
            @Valid @RequestBody CreateTrustedContactRequest request,
            Authentication authentication
    ) {
        return trustedContactService.createTrustedContact(request, authentication);
    }

    @PutMapping("/{contactId}")
    public TrustedContactResponseDto update(
            @PathVariable Long contactId,
            @Valid @RequestBody UpdateTrustedContactRequest request,
            Authentication authentication
    ) {
        return trustedContactService.updateTrustedContact(contactId, request, authentication);
    }

    @PatchMapping("/{contactId}/toggle-status")
    public TrustedContactResponseDto toggleStatus(@PathVariable Long contactId, Authentication authentication) {
        return trustedContactService.toggleTrustedContactStatus(contactId, authentication);
    }

    @DeleteMapping("/{contactId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long contactId, Authentication authentication) {
        trustedContactService.deleteTrustedContact(contactId, authentication);
    }
}
