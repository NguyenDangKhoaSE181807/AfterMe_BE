package com.example.reminder.service.impl;

import com.example.reminder.dto.trustedcontact.CreateTrustedContactRequest;
import com.example.reminder.dto.trustedcontact.TrustedContactResponseDto;
import com.example.reminder.dto.trustedcontact.UpdateTrustedContactRequest;
import com.example.reminder.entity.TrustedContact;
import com.example.reminder.entity.User;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.repository.TrustedContactRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.TrustedContactService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrustedContactServiceImpl implements TrustedContactService {

    private final TrustedContactRepository trustedContactRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TrustedContactResponseDto> getAllTrustedContacts(Authentication authentication) {
        Long userId = resolveCurrentUserId(authentication);
        return trustedContactRepository.findByUserIdAndDeletedAtIsNull(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TrustedContactResponseDto getTrustedContactById(Long contactId, Authentication authentication) {
        Long userId = resolveCurrentUserId(authentication);
        return toDto(loadOwnedContact(userId, contactId));
    }

    @Override
    @Transactional
    public TrustedContactResponseDto createTrustedContact(CreateTrustedContactRequest request, Authentication authentication) {
        Long userId = resolveCurrentUserId(authentication);
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        TrustedContact trustedContact = new TrustedContact();
        trustedContact.setUser(user);
        trustedContact.setFullName(request.fullName());
        trustedContact.setRelationship(request.relationship());
        trustedContact.setPhone(request.phone());
        trustedContact.setEmail(request.email());
        trustedContact.setIsActive(true);
        trustedContact.setCreatedAt(LocalDateTime.now());
        return toDto(trustedContactRepository.save(trustedContact));
    }

    @Override
    @Transactional
    public TrustedContactResponseDto updateTrustedContact(
            Long contactId,
            UpdateTrustedContactRequest request,
            Authentication authentication
    ) {
        Long userId = resolveCurrentUserId(authentication);
        TrustedContact trustedContact = loadOwnedContact(userId, contactId);

        if (request.fullName() != null) {
            trustedContact.setFullName(request.fullName());
        }
        if (request.relationship() != null) {
            trustedContact.setRelationship(request.relationship());
        }
        if (request.phone() != null) {
            trustedContact.setPhone(request.phone());
        }
        if (request.email() != null) {
            trustedContact.setEmail(request.email());
        }
        trustedContact.setIsActive(request.isActive());

        return toDto(trustedContactRepository.save(trustedContact));
    }

    @Override
    @Transactional
    public void deleteTrustedContact(Long contactId, Authentication authentication) {
        Long userId = resolveCurrentUserId(authentication);
        TrustedContact trustedContact = loadOwnedContact(userId, contactId);
        trustedContact.setDeletedAt(LocalDateTime.now());
        trustedContact.setIsActive(false);
        trustedContactRepository.save(trustedContact);
    }

    @Override
    @Transactional
    public TrustedContactResponseDto toggleTrustedContactStatus(Long contactId, Authentication authentication) {
        Long userId = resolveCurrentUserId(authentication);
        TrustedContact trustedContact = loadOwnedContact(userId, contactId);
        trustedContact.setIsActive(!Boolean.TRUE.equals(trustedContact.getIsActive()));
        return toDto(trustedContactRepository.save(trustedContact));
    }

    private TrustedContact loadOwnedContact(Long userId, Long contactId) {
        return trustedContactRepository.findById(contactId)
                .filter(contact -> contact.getDeletedAt() == null)
                .filter(contact -> contact.getUser() != null && userId.equals(contact.getUser().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Trusted contact not found: " + contactId));
    }

    private Long resolveCurrentUserId(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            throw new AccessDeniedException("User is not authenticated");
        }

        Jwt jwt = jwtAuthenticationToken.getToken();
        Object uidClaim = jwt.getClaims().get("uid");
        if (uidClaim instanceof Number number) {
            return number.longValue();
        }
        if (uidClaim instanceof String uidString && !uidString.isBlank()) {
            return Long.parseLong(uidString);
        }

        throw new AccessDeniedException("User id claim is missing");
    }

    private TrustedContactResponseDto toDto(TrustedContact trustedContact) {
        return new TrustedContactResponseDto(
                trustedContact.getId(),
                trustedContact.getUser().getId(),
                trustedContact.getFullName(),
                trustedContact.getRelationship(),
                trustedContact.getPhone(),
                trustedContact.getEmail(),
                trustedContact.getIsActive(),
                trustedContact.getCreatedAt()
        );
    }
}
