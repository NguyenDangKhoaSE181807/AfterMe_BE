package com.example.reminder.service.impl;

import com.example.reminder.dto.trustedcontact.CreateTrustedContactRequest;
import com.example.reminder.dto.trustedcontact.TrustedContactResponseDto;
import com.example.reminder.dto.trustedcontact.UpdateTrustedContactRequest;
import com.example.reminder.entity.Plan;
import com.example.reminder.entity.TrustedContact;
import com.example.reminder.entity.User;
import com.example.reminder.exception.BadRequestException;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.repository.PlanRepository;
import com.example.reminder.repository.TrustedContactRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.TrustedContactService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrustedContactServiceImpl implements TrustedContactService {

    private final TrustedContactRepository trustedContactRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TrustedContactResponseDto> getAllTrustedContacts(Authentication authentication) {
        Long userId = resolveCurrentUserId(authentication);
        return trustedContactRepository.findByUserIdAndDeletedAtIsNullOrderByPriorityAscCreatedAtAsc(userId)
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
        validateTrustedContactLimit(user);
        int priority = resolvePriority(request.priority());
        validatePriorityAvailable(userId, priority, null);

        TrustedContact trustedContact = new TrustedContact();
        trustedContact.setUser(user);
        trustedContact.setFullName(request.fullName());
        trustedContact.setRelationship(request.relationship());
        trustedContact.setPhone(request.phone());
        trustedContact.setEmail(request.email());
        trustedContact.setPriority(priority);
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
        if (request.priority() != null) {
            int priority = resolvePriority(request.priority());
            validatePriorityAvailable(userId, priority, trustedContact.getId());
            trustedContact.setPriority(priority);
        }
        if (Boolean.TRUE.equals(request.isActive()) && !Boolean.TRUE.equals(trustedContact.getIsActive())) {
            validateTrustedContactLimit(trustedContact.getUser());
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
        if (!Boolean.TRUE.equals(trustedContact.getIsActive())) {
            validateTrustedContactLimit(trustedContact.getUser());
        }
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
                trustedContact.getPriority(),
                trustedContact.getIsActive(),
                trustedContact.getCreatedAt()
        );
    }

    private void validateTrustedContactLimit(User user) {
        int maxTrustedContacts = resolveMaxTrustedContacts(user);
        long currentActiveContacts = trustedContactRepository.countByUserIdAndDeletedAtIsNullAndIsActiveTrue(user.getId());
        if (currentActiveContacts >= maxTrustedContacts) {
            throw new BadRequestException("Trusted contact limit reached for current plan. Maximum allowed: " + maxTrustedContacts);
        }
    }

    private int resolveMaxTrustedContacts(User user) {
        Plan plan = user.getCurrentPlan();
        if (plan == null) {
            plan = planRepository.findByNameIgnoreCaseAndDeletedAtIsNull("FREE")
                    .orElse(null);
        }
        if (plan == null || plan.getMaxTrustedContacts() == null) {
            return 1;
        }
        return Math.max(0, plan.getMaxTrustedContacts());
    }

    private int resolvePriority(Integer priority) {
        return priority == null ? 1 : Math.max(1, priority);
    }

    private void validatePriorityAvailable(Long userId, Integer priority, Long currentContactId) {
        boolean priorityTaken = currentContactId == null
                ? trustedContactRepository.existsByUserIdAndPriorityAndDeletedAtIsNull(userId, priority)
                : trustedContactRepository.existsByUserIdAndPriorityAndDeletedAtIsNullAndIdNot(userId, priority, currentContactId);
        if (priorityTaken) {
            throw new BadRequestException("Trusted contact priority already exists: " + priority);
        }
    }
}
