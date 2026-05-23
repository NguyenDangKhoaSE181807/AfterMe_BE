package com.example.reminder.service.impl;

import com.example.reminder.dto.trustedcontact.CreateTrustedContactRequest;
import com.example.reminder.dto.trustedcontact.TrustedContactResponseDto;
import com.example.reminder.dto.trustedcontact.UpdateTrustedContactRequest;
import com.example.reminder.entity.TrustedContact;
import com.example.reminder.entity.User;
import com.example.reminder.exception.BadRequestException;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.repository.TrustedContactRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.TrustedContactService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrustedContactServiceImpl implements TrustedContactService {

    private final TrustedContactRepository trustedContactRepository;
    private final UserRepository userRepository;

    @Override
    public List<TrustedContactResponseDto> getAllTrustedContacts(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return trustedContactRepository.findByUserIdAndDeletedAtIsNull(user.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public TrustedContactResponseDto getTrustedContactById(Long contactId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        TrustedContact contact = trustedContactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Trusted contact not found: " + contactId));

        if (contact.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Trusted contact not found: " + contactId);
        }

        if (!contact.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }

        return toDto(contact);
    }

    @Override
    public TrustedContactResponseDto createTrustedContact(CreateTrustedContactRequest request, Authentication authentication) {
        User user = getCurrentUser(authentication);

        Integer maxTrusted = null;
        if (user.getCurrentPlan() != null) {
            maxTrusted = user.getCurrentPlan().getMaxTrustedContacts();
        }

        if (maxTrusted != null) {
            long activeCount = trustedContactRepository.countByUserIdAndDeletedAtIsNullAndIsActiveTrue(user.getId());
            if (activeCount >= maxTrusted) {
                throw new BadRequestException("Maximum trusted contacts reached");
            }
        }

        TrustedContact contact = new TrustedContact();
        contact.setUser(user);
        contact.setFullName(request.fullName());
        contact.setRelationship(request.relationship());
        contact.setPhone(request.phone());
        contact.setEmail(request.email());
        contact.setIsActive(Boolean.TRUE);
        contact.setCreatedAt(LocalDateTime.now());
        contact.setDeletedAt(null);

        return toDto(trustedContactRepository.save(contact));
    }

    @Override
    public TrustedContactResponseDto updateTrustedContact(Long contactId, UpdateTrustedContactRequest request, Authentication authentication) {
        User user = getCurrentUser(authentication);
        TrustedContact existing = trustedContactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Trusted contact not found: " + contactId));

        if (existing.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Trusted contact not found: " + contactId);
        }

        if (!existing.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }

        if (request.fullName() != null) existing.setFullName(request.fullName());
        if (request.relationship() != null) existing.setRelationship(request.relationship());
        if (request.phone() != null) existing.setPhone(request.phone());
        if (request.email() != null) existing.setEmail(request.email());
        if (request.isActive() != null) existing.setIsActive(request.isActive());

        return toDto(trustedContactRepository.save(existing));
    }

    @Override
    public void deleteTrustedContact(Long contactId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        TrustedContact existing = trustedContactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Trusted contact not found: " + contactId));

        if (existing.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Trusted contact not found: " + contactId);
        }

        if (!existing.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }

        existing.setDeletedAt(LocalDateTime.now());
        existing.setIsActive(Boolean.FALSE);
        trustedContactRepository.save(existing);
    }

    @Override
    public TrustedContactResponseDto toggleTrustedContactStatus(Long contactId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        TrustedContact existing = trustedContactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Trusted contact not found: " + contactId));

        if (existing.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Trusted contact not found: " + contactId);
        }

        if (!existing.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }

        existing.setIsActive(!Boolean.TRUE.equals(existing.getIsActive()));
        return toDto(trustedContactRepository.save(existing));
    }

    private TrustedContactResponseDto toDto(TrustedContact contact) {
        return new TrustedContactResponseDto(
                contact.getId(),
                contact.getUser() == null ? null : contact.getUser().getId(),
                contact.getFullName(),
                contact.getRelationship(),
                contact.getPhone(),
                contact.getEmail(),
                contact.getIsActive(),
                contact.getCreatedAt()
        );
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User must be authenticated");
        }

        String email = authentication.getName();
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}
