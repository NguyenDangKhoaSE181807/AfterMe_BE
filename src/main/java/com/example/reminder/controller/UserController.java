package com.example.reminder.controller;

import com.example.reminder.dto.user.CreateUserRequest;
import com.example.reminder.dto.user.UpdateUserRequest;
import com.example.reminder.dto.user.UserResponseDto;
import com.example.reminder.dto.user.CreateUserCommand;
import com.example.reminder.dto.user.UpdateUserCommand;
import com.example.reminder.domain.model.UserModel;
import com.example.reminder.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import lombok.RequiredArgsConstructor;

import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public Page<UserResponseDto> findAll(
        @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        return userService.findAll(pageable).map(this::toDto);
    }

    @GetMapping("/{id}")
    public UserResponseDto findById(@PathVariable Long id) {
        return toDto(userService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto create(@Valid @RequestBody CreateUserRequest request) {
        CreateUserCommand command = new CreateUserCommand(
                request.email(),
                request.passwordHash(),
                request.fullName(),
                request.tonePreference(),
            request.status(),
            request.role()
        );

        return toDto(userService.create(command));
    }

    @PutMapping("/{id}")
    public UserResponseDto update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        UpdateUserCommand command = new UpdateUserCommand(
                request.email(),
                request.passwordHash(),
                request.fullName(),
                request.tonePreference(),
            request.status(),
            request.role()
        );

        return toDto(userService.update(id, command));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }

    private UserResponseDto toDto(UserModel user) {
        return new UserResponseDto(
                user.id(),
                user.email(),
                user.fullName(),
                user.tonePreference(),
                user.status(),
                user.role(),
                user.createdAt()
        );
    }
}





