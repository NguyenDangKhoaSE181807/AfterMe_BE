package com.example.reminder.service.impl;

import com.example.reminder.dto.user.CreateUserCommand;
import com.example.reminder.dto.user.UpdateUserCommand;
import com.example.reminder.exception.BadRequestException;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.domain.model.UserModel;
import com.example.reminder.entity.User;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public List<UserModel> findAll() {
        return userRepository.findAllByDeletedAtIsNull().stream().map(this::toModel).toList();
    }

    @Override
    public UserModel findById(Long id) {
        return getActiveUser(id);
    }

    @Override
    public UserModel create(CreateUserCommand command) {
        if (userRepository.existsByEmailAndDeletedAtIsNull(command.email())) {
            throw new BadRequestException("Email already exists");
        }

        User user = new User();
        user.setEmail(command.email());
        user.setPasswordHash(command.passwordHash());
        user.setFullName(command.fullName());
        user.setTonePreference(command.tonePreference());
        user.setStatus(command.status());
        user.setCreatedAt(LocalDateTime.now());

        return toModel(userRepository.save(user));
    }

    @Override
    public UserModel update(Long id, UpdateUserCommand command) {
        User user = getActiveUserEntity(id);

        if (!user.getEmail().equalsIgnoreCase(command.email())
                && userRepository.existsByEmailAndDeletedAtIsNull(command.email())) {
            throw new BadRequestException("Email already exists");
        }

        user.setEmail(command.email());
        user.setPasswordHash(command.passwordHash());
        user.setFullName(command.fullName());
        user.setTonePreference(command.tonePreference());
        user.setStatus(command.status());
        user.setDeletedAt(null);

        return toModel(userRepository.save(user));
    }

    @Override
    public void delete(Long id) {
        User user = getActiveUserEntity(id);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public UserModel getActiveUser(Long id) {
        return toModel(getActiveUserEntity(id));
    }

    private User getActiveUserEntity(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private UserModel toModel(User user) {
        return new UserModel(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getTonePreference(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getDeletedAt()
        );
    }
}
