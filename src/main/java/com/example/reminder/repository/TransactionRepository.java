package com.example.reminder.repository;

import com.example.reminder.entity.Transaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserIdAndDeletedAtIsNull(Long userId);
}
