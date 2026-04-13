package com.example.reminder.controller;

import com.example.reminder.dto.habit.CreateHabitRequest;
import com.example.reminder.dto.habit.HabitResponseDto;
import com.example.reminder.dto.habit.UpdateHabitRequest;
import com.example.reminder.dto.habit.CreateHabitCommand;
import com.example.reminder.dto.habit.UpdateHabitCommand;
import com.example.reminder.domain.model.HabitModel;
import com.example.reminder.service.HabitService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/habits")
@RequiredArgsConstructor
public class HabitController {

    private final HabitService habitService;

    @GetMapping
    public List<HabitResponseDto> findAll(@RequestParam(required = false) Long userId) {
        return habitService.findAll(userId).stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public HabitResponseDto findById(@PathVariable Long id) {
        return toDto(habitService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HabitResponseDto create(@Valid @RequestBody CreateHabitRequest request) {
        CreateHabitCommand command = new CreateHabitCommand(
                request.userId(),
                request.name(),
                request.category()
        );

        return toDto(habitService.create(command));
    }

    @PutMapping("/{id}")
    public HabitResponseDto update(@PathVariable Long id, @Valid @RequestBody UpdateHabitRequest request) {
        UpdateHabitCommand command = new UpdateHabitCommand(
                request.userId(),
                request.name(),
                request.category()
        );

        return toDto(habitService.update(id, command));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        habitService.delete(id);
    }

    private HabitResponseDto toDto(HabitModel habit) {
        return new HabitResponseDto(
                habit.id(),
                habit.userId(),
                habit.name(),
                habit.category(),
                habit.createdAt()
        );
    }
}





