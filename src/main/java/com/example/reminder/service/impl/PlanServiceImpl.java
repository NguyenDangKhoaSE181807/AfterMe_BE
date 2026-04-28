package com.example.reminder.service.impl;

import com.example.reminder.entity.Plan;
import com.example.reminder.repository.PlanRepository;
import com.example.reminder.service.PlanService;
import com.example.reminder.dto.plan.CreatePlanCommand;
import com.example.reminder.dto.plan.UpdatePlanCommand;
import com.example.reminder.dto.plan.PlanResponseDto;
import java.util.List;
import java.time.LocalDateTime;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;

    @Override
    public List<PlanResponseDto> findAllActive() {
        return planRepository.findByIsActiveTrueAndDeletedAtIsNull()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public PlanResponseDto findById(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + id));
        return toDto(plan);
    }

    @Override
    public PlanResponseDto create(Long userId, CreatePlanCommand command) {
        if (planRepository.existsByNameAndDeletedAtIsNull(command.name())) {
            throw new BadRequestException("Plan name already exists");
        }

        Plan plan = new Plan();
        plan.setName(command.name());
        plan.setPrice(command.price());
        plan.setBillingCycle(command.billingCycle());
        plan.setMaxReminders(command.maxReminders());
        plan.setMaxTrustedContacts(command.maxTrustedContacts());
        plan.setMaxDigitalAssets(command.maxDigitalAssets());
        plan.setFeatures(command.features());
        plan.setIsActive(command.isActive() == null ? Boolean.TRUE : command.isActive());
        plan.setCreatedAt(LocalDateTime.now());
        plan.setDeletedAt(null);

        return toDto(planRepository.save(plan));
    }

    @Override
    public PlanResponseDto update(Long userId, Long id, UpdatePlanCommand command) {
        Plan existing = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + id));

        existing.setName(command.name());
        existing.setPrice(command.price());
        existing.setBillingCycle(command.billingCycle());
        existing.setMaxReminders(command.maxReminders());
        existing.setMaxTrustedContacts(command.maxTrustedContacts());
        existing.setMaxDigitalAssets(command.maxDigitalAssets());
        existing.setFeatures(command.features());
        if (command.isActive() != null) {
            existing.setIsActive(command.isActive());
        }

        return toDto(planRepository.save(existing));
    }

    @Override
    public void deleteById(Long userId, Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + id));

        if (!Boolean.TRUE.equals(plan.getIsActive())) {
            throw new BadRequestException("Plan is not active and cannot be deleted");
        }

        plan.setDeletedAt(LocalDateTime.now());
        plan.setIsActive(false);
        planRepository.save(plan);
    }

    private PlanResponseDto toDto(Plan plan) {
        return new PlanResponseDto(
                plan.getId(),
                plan.getName(),
                plan.getPrice(),
                plan.getBillingCycle(),
                plan.getMaxReminders(),
                plan.getMaxTrustedContacts(),
                plan.getMaxDigitalAssets(),
                plan.getFeatures(),
                plan.getIsActive(),
                plan.getCreatedAt()
        );
    }
}
