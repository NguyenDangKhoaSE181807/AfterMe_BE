package com.example.reminder.service.impl;

import com.example.reminder.dto.transaction.TransactionResponseDto;
import com.example.reminder.entity.Transaction;
import com.example.reminder.entity.User;
import com.example.reminder.exception.ForbiddenException;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.repository.TransactionRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.TransactionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getAllTransactions(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);

        List<Transaction> transactions = transactionRepository.findByUserId(currentUser.getId());

        return transactions.stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponseDto getTransactionById(Long transactionId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        // Check if the transaction belongs to the current user
        if (!transaction.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You don't have permission to access this transaction");
        }

        return mapToResponseDto(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getTransactionsByUserId(Long userId, Authentication authentication) {
        ensureAdmin(authentication);

        List<Transaction> transactions = transactionRepository.findByUserId(userId);

        return transactions.stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getAllTransactionsForAdmin(Authentication authentication) {
        ensureAdmin(authentication);

        List<Transaction> transactions = transactionRepository.findAll();

        return transactions.stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    private User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void ensureAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User must be authenticated");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .map(Object::toString)
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a) || "ADMIN".equalsIgnoreCase(a));

        if (!isAdmin) {
            throw new ForbiddenException("Only admin can access this resource");
        }
    }

    private TransactionResponseDto mapToResponseDto(Transaction transaction) {
        return new TransactionResponseDto(
                transaction.getId(),
                transaction.getUser().getId(),
                transaction.getSubscription().getId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getPaymentMethod(),
                transaction.getStatus(),
                transaction.getTransactionRef(),
                transaction.getPaidAt(),
                transaction.getCreatedAt()
        );
    }
}
