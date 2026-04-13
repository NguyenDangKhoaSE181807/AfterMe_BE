package com.example.reminder.controller;

import com.example.reminder.dto.reminder.CreateReminderRequest;
import com.example.reminder.dto.reminder.ReminderResponseDto;
import com.example.reminder.dto.reminder.UpdateReminderRequest;
import com.example.reminder.dto.reminder.CreateReminderCommand;
import com.example.reminder.dto.reminder.UpdateReminderCommand;
import com.example.reminder.domain.model.ReminderModel;
import com.example.reminder.service.ReminderService;
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
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    @GetMapping
    public List<ReminderResponseDto> findAll(@RequestParam(required = false) Long userId) {
        return reminderService.findAll(userId).stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public ReminderResponseDto findById(@PathVariable Long id) {
        return toDto(reminderService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReminderResponseDto create(@Valid @RequestBody CreateReminderRequest request) {
        CreateReminderCommand command = new CreateReminderCommand(
                request.userId(),
                request.habitId(),
                request.title(),
                request.description(),
                request.tone(),
                request.safetyEnabled(),
                request.status()
        );

        return toDto(reminderService.create(command));
    }

    @PutMapping("/{id}")
    public ReminderResponseDto update(@PathVariable Long id, @Valid @RequestBody UpdateReminderRequest request) {
        UpdateReminderCommand command = new UpdateReminderCommand(
                request.userId(),
                request.habitId(),
                request.title(),
                request.description(),
                request.tone(),
                request.safetyEnabled(),
                request.status()
        );

        return toDto(reminderService.update(id, command));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        reminderService.delete(id);
    }

    private ReminderResponseDto toDto(ReminderModel reminder) {
        return new ReminderResponseDto(
                reminder.id(),
                reminder.userId(),
                reminder.habitId(),
                reminder.title(),
                reminder.description(),
                reminder.tone(),
                reminder.safetyEnabled(),
                reminder.status(),
                reminder.createdAt()
        );
    }
}





