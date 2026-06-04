package com.example.reminder.repository;

import com.example.reminder.entity.Plan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    List<Plan> findByDeletedAtIsNull();

    Optional<Plan> findByNameIgnoreCaseAndDeletedAtIsNull(String name);

    boolean existsByNameAndDeletedAtIsNull(String name);

    long countByDeletedAtIsNull();

    long countByDeletedAtIsNullAndIsActiveTrue();
}
