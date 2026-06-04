package com.example.reminder.repository;

import com.example.reminder.dto.subscription.UserSubscriptionDto;
import com.example.reminder.entity.Transaction;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    List<Transaction> findByUserIdAndDeletedAtIsNull(Long userId);

    long countByDeletedAtIsNull();

    long countByDeletedAtIsNullAndStatusIgnoreCase(String status);

    List<Transaction> findByDeletedAtIsNullOrderByCreatedAtDesc();

    List<Transaction> findTop10ByDeletedAtIsNullOrderByCreatedAtDesc();

    Optional<Transaction> findByIdAndDeletedAtIsNull(Long id);

    List<Transaction> findByDeletedAtIsNullAndStatusIgnoreCaseAndPaidAtBetweenOrderByPaidAtAsc(
        String status,
        LocalDateTime from,
        LocalDateTime to
    );

    @Query("""
        select coalesce(sum(t.amount), 0)
        from Transaction t
        where t.deletedAt is null
          and lower(t.status) = lower(:status)
          and t.paidAt between :from and :to
        """)
    BigDecimal sumAmountByStatusAndPaidAtBetween(
        @Param("status") String status,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    @Query("""
            select transaction
              from Transaction transaction
              join transaction.user user
             where transaction.deletedAt is null
               and (:status is null or lower(transaction.status) = :status)
               and (:qPattern is null
                    or lower(user.email) like :qPattern
                    or lower(user.fullName) like :qPattern
                    or lower(transaction.transactionRef) like :qPattern)
               and (:from is null or transaction.createdAt >= :from)
               and (:to is null or transaction.createdAt < :to)
            """)
    Page<Transaction> searchAdminTransactions(
            @Param("qPattern") String qPattern,
            @Param("status") String status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    @Query("""
            select transaction
              from Transaction transaction
             where transaction.deletedAt is null
               and transaction.createdAt >= :from
               and transaction.createdAt < :to
             order by transaction.createdAt asc
            """)
    List<Transaction> findCreatedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

   Optional<Transaction> findFirstByTransactionRefAndPaymentMethod(String transactionRef, String paymentMethod);
    List<Transaction> findByUserId(Long id);

        List<Transaction> findByPaymentMethodAndStatusAndCreatedAtAfter(String paymentMethod, String status, LocalDateTime createdAfter);
}
