package com.example.reminder.service;

import com.example.reminder.dto.plan.CreatePlanCommand;
import com.example.reminder.dto.plan.UpdatePlanCommand;
import com.example.reminder.dto.plan.PlanResponseDto;
import java.util.List;
import org.springframework.security.core.Authentication;

public interface PlanService {

    List<PlanResponseDto> findAllActive();

    PlanResponseDto findById(Long id);

    PlanResponseDto create(Authentication authentication, CreatePlanCommand command);

    PlanResponseDto update(Authentication authentication, Long id, UpdatePlanCommand command);

    void deleteById(Authentication authentication, Long id);
}
