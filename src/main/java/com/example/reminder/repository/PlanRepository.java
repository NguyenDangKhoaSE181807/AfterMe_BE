package com.example.reminder.repository;

import com.example.reminder.entity.Plan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    List<Plan> findByIsActiveTrueAndDeletedAtIsNull();

    boolean existsByNameAndDeletedAtIsNull(String name);
}
