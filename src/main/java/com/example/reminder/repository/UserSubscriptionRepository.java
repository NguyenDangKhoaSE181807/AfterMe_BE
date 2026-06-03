package com.example.reminder.repository;

import com.example.reminder.entity.UserSubscription;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    List<UserSubscription> findByUserIdAndDeletedAtIsNull(Long userId);

    Optional<UserSubscription> findByIdAndDeletedAtIsNull(Long id);

    Optional<UserSubscription> findFirstByUserIdAndDeletedAtIsNullAndStatusAndEndAtGreaterThanOrderByStartAtDesc(
            Long userId,
            String status,
            LocalDateTime now
    );

    List<UserSubscription> findByStatusAndEndAtLessThanEqual(
            String status,
            LocalDateTime now
    );

    List<UserSubscription> findByUserIdAndDeletedAtIsNullOrderByStartAtDesc(Long userId);

        long countByDeletedAtIsNull();

        long countByDeletedAtIsNullAndStatus(String status);

        long countByDeletedAtIsNullAndCreatedAtGreaterThanEqual(LocalDateTime from);

        List<UserSubscription> findByUserIdAndDeletedAtIsNullAndStatusIn(Long userId, List<String> statuses);

        @Query("""
                select subscription
                  from UserSubscription subscription
                  join subscription.user user
                  join subscription.plan plan
                 where (:qPattern is null
                        or lower(user.email) like :qPattern
                        or lower(user.fullName) like :qPattern)
                   and (:status is null or lower(subscription.status) = :status)
                   and (:planId is null or plan.id = :planId)
                """)
        Page<UserSubscription> searchAdminSubscriptions(
                @Param("qPattern") String qPattern,
                @Param("status") String status,
                @Param("planId") Long planId,
                Pageable pageable
        );

        @Query("""
                select coalesce(sum(plan.price), 0)
                  from UserSubscription subscription
                  join subscription.plan plan
                 where subscription.deletedAt is null
                   and lower(subscription.status) = lower(:status)
                   and subscription.endAt > :now
                """)
        java.math.BigDecimal sumActiveSubscriptionPlanPrices(
                @Param("status") String status,
                @Param("now") LocalDateTime now
        );
}
