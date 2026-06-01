package com.example.reminder.repository;

import com.example.reminder.entity.Transaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserId(Long userId);
    
    
    List<Transaction> findByPaymentMethodAndStatusAndCreatedAtAfter(String paymentMethod, String status, LocalDateTime createdAfter);

    Optional<Transaction> findFirstByTransactionRefAndPaymentMethod(String transactionRef, String paymentMethod);
}