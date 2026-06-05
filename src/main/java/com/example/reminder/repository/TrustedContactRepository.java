package com.example.reminder.repository;

import com.example.reminder.entity.TrustedContact;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrustedContactRepository extends JpaRepository<TrustedContact, Long> {

    List<TrustedContact> findByUserIdAndDeletedAtIsNull(Long userId);

    List<TrustedContact> findByUserIdAndDeletedAtIsNullOrderByPriorityAscCreatedAtAsc(Long userId);

    List<TrustedContact> findByUserIdAndDeletedAtIsNullAndIsActiveTrueOrderByPriorityAscCreatedAtAsc(Long userId);

    long countByUserIdAndDeletedAtIsNullAndIsActiveTrue(Long userId);

    boolean existsByUserIdAndPriorityAndDeletedAtIsNull(Long userId, Integer priority);

    boolean existsByUserIdAndPriorityAndDeletedAtIsNullAndIdNot(Long userId, Integer priority, Long id);
}





