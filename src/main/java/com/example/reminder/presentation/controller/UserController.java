package com.example.reminder.presentation.controller;

import com.example.reminder.presentation.dto.user.CreateUserRequest;
import com.example.reminder.presentation.dto.user.UpdateUserRequest;
import com.example.reminder.presentation.dto.user.UserResponseDto;
import com.example.reminder.application.dto.user.CreateUserCommand;
import com.example.reminder.application.dto.user.UpdateUserCommand;
import com.example.reminder.application.usecase.UserCrudUseCase;
import com.example.reminder.domain.model.UserModel;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserCrudUseCase userCrudUseCase;

    @GetMapping
    public List<UserResponseDto> findAll() {
        return userCrudUseCase.findAll().stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public UserResponseDto findById(@PathVariable Long id) {
        return toDto(userCrudUseCase.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto create(@Valid @RequestBody CreateUserRequest request) {
        CreateUserCommand command = new CreateUserCommand(
                request.email(),
                request.passwordHash(),
                request.fullName(),
                request.tonePreference(),
                request.status()
        );

        return toDto(userCrudUseCase.create(command));
    }

    @PutMapping("/{id}")
    public UserResponseDto update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        UpdateUserCommand command = new UpdateUserCommand(
                request.email(),
                request.passwordHash(),
                request.fullName(),
                request.tonePreference(),
                request.status()
        );

        return toDto(userCrudUseCase.update(id, command));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userCrudUseCase.delete(id);
    }

    private UserResponseDto toDto(UserModel user) {
        return new UserResponseDto(
                user.id(),
                user.email(),
                user.fullName(),
                user.tonePreference(),
                user.status(),
                user.createdAt()
        );
    }
}





