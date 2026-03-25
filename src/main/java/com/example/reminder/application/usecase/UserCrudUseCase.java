package com.example.reminder.application.usecase;

import com.example.reminder.application.dto.user.CreateUserCommand;
import com.example.reminder.application.dto.user.UpdateUserCommand;
import com.example.reminder.application.port.out.UserPersistencePort;
import com.example.reminder.domain.model.UserModel;
import com.example.reminder.application.exception.BadRequestException;
import com.example.reminder.application.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
public class UserCrudUseCase {

    private final UserPersistencePort userPersistencePort;

    public UserCrudUseCase(UserPersistencePort userPersistencePort) {
        this.userPersistencePort = userPersistencePort;
    }

    public List<UserModel> findAll() {
        return userPersistencePort.findAllActive();
    }

    public UserModel findById(Long id) {
        return getActiveUser(id);
    }

    public UserModel create(CreateUserCommand command) {
        if (userPersistencePort.existsActiveByEmail(command.email())) {
            throw new BadRequestException("Email already exists");
        }

        UserModel user = new UserModel(
                null,
                command.email(),
                command.passwordHash(),
                command.fullName(),
                command.tonePreference(),
                command.status(),
                LocalDateTime.now(),
                null
        );

        return userPersistencePort.save(user);
    }

    public UserModel update(Long id, UpdateUserCommand command) {
        UserModel current = getActiveUser(id);

        if (!current.email().equalsIgnoreCase(command.email())
                && userPersistencePort.existsActiveByEmail(command.email())) {
            throw new BadRequestException("Email already exists");
        }

        UserModel updated = new UserModel(
                current.id(),
                command.email(),
                command.passwordHash(),
                command.fullName(),
                command.tonePreference(),
                command.status(),
                current.createdAt(),
                null
        );

        return userPersistencePort.save(updated);
    }

    public void delete(Long id) {
        UserModel current = getActiveUser(id);

        UserModel deleted = new UserModel(
                current.id(),
                current.email(),
                current.passwordHash(),
                current.fullName(),
                current.tonePreference(),
                current.status(),
                current.createdAt(),
                LocalDateTime.now()
        );

        userPersistencePort.save(deleted);
    }

    public UserModel getActiveUser(Long id) {
        return userPersistencePort.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}




