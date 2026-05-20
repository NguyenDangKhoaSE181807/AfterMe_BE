package com.example.reminder.service;

import com.example.reminder.dto.transaction.TransactionResponseDto;
import java.util.List;
import org.springframework.security.core.Authentication;

public interface TransactionService {

    /**
     * Get all transactions for the authenticated user
     */
    List<TransactionResponseDto> getAllTransactions(Authentication authentication);

    /**
     * Get transaction by ID
     */
    TransactionResponseDto getTransactionById(Long transactionId, Authentication authentication);

    /**
     * Get all transactions for a specific user (admin only)
     */
    List<TransactionResponseDto> getTransactionsByUserId(Long userId);
}
