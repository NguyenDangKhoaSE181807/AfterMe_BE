package com.example.reminder.controller;

import com.example.reminder.dto.common.BaseResponse;
import com.example.reminder.dto.transaction.TransactionResponseDto;
import com.example.reminder.service.TransactionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Get all transactions for the authenticated user
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<TransactionResponseDto>>> getAllTransactions(
            Authentication authentication
    ) {
        List<TransactionResponseDto> transactions = transactionService.getAllTransactions(authentication);

        BaseResponse<List<TransactionResponseDto>> response = BaseResponse.<List<TransactionResponseDto>>builder()
                .code("GET_TRANSACTIONS_SUCCESS")
                .message("Transactions retrieved successfully")
                .data(transactions)
                .timestamp(java.time.Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Get transaction by ID
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<BaseResponse<TransactionResponseDto>> getTransactionById(
            @PathVariable Long transactionId,
            Authentication authentication
    ) {
        TransactionResponseDto transaction = transactionService.getTransactionById(transactionId, authentication);

        BaseResponse<TransactionResponseDto> response = BaseResponse.<TransactionResponseDto>builder()
                .code("GET_TRANSACTION_SUCCESS")
                .message("Transaction retrieved successfully")
                .data(transaction)
                .timestamp(java.time.Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Get all transactions for a specific user (admin only)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<BaseResponse<List<TransactionResponseDto>>> getTransactionsByUserId(
            @PathVariable Long userId
    ) {
        List<TransactionResponseDto> transactions = transactionService.getTransactionsByUserId(userId);

        BaseResponse<List<TransactionResponseDto>> response = BaseResponse.<List<TransactionResponseDto>>builder()
                .code("GET_USER_TRANSACTIONS_SUCCESS")
                .message("User transactions retrieved successfully")
                .data(transactions)
                .timestamp(java.time.Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
