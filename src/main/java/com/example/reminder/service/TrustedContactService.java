package com.example.reminder.service;

import com.example.reminder.dto.trustedcontact.CreateTrustedContactRequest;
import com.example.reminder.dto.trustedcontact.TrustedContactResponseDto;
import com.example.reminder.dto.trustedcontact.UpdateTrustedContactRequest;
import java.util.List;
import org.springframework.security.core.Authentication;

public interface TrustedContactService {

    /**
     * Get all trusted contacts for the authenticated user
     */
    List<TrustedContactResponseDto> getAllTrustedContacts(Authentication authentication);

    /**
     * Get a specific trusted contact by ID
     */
    TrustedContactResponseDto getTrustedContactById(Long contactId, Authentication authentication);

    /**
     * Create a new trusted contact
     */
    TrustedContactResponseDto createTrustedContact(CreateTrustedContactRequest request, Authentication authentication);

    /**
     * Update a trusted contact
     */
    TrustedContactResponseDto updateTrustedContact(Long contactId, UpdateTrustedContactRequest request, Authentication authentication);

    /**
     * Delete (soft delete) a trusted contact
     */
    void deleteTrustedContact(Long contactId, Authentication authentication);

    /**
     * Activate/Deactivate a trusted contact
     */
    TrustedContactResponseDto toggleTrustedContactStatus(Long contactId, Authentication authentication);
}
