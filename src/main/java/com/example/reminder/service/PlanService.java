package com.example.reminder.service;

import com.example.reminder.dto.plan.CreatePlanCommand;
import com.example.reminder.dto.plan.UpdatePlanCommand;
import com.example.reminder.dto.plan.PlanResponseDto;
import java.util.List;

public interface PlanService {

    List<PlanResponseDto> findAllActive();

    PlanResponseDto findById(Long id);

    PlanResponseDto create(Long userId, CreatePlanCommand command);

    PlanResponseDto update(Long userId, Long id, UpdatePlanCommand command);

    void deleteById(Long userId, Long id);
}
