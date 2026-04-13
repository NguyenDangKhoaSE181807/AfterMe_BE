package com.example.reminder.service.impl;

import com.example.reminder.entity.Plan;
import com.example.reminder.repository.PlanRepository;
import com.example.reminder.service.PlanService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;

    @Override
    public List<Plan> findAllActive() {
        return planRepository.findByIsActiveTrueAndDeletedAtIsNull();
    }

    @Override
    public Optional<Plan> findById(Long id) {
        return planRepository.findById(id);
    }
}
