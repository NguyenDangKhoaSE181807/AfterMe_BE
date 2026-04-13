package com.example.reminder.service;

import com.example.reminder.entity.Plan;
import java.util.List;
import java.util.Optional;

public interface PlanService {

    List<Plan> findAllActive();

    Optional<Plan> findById(Long id);
}
