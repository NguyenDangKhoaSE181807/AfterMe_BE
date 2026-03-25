package com.example.reminder.infrastructure.persistence.adapter;

import com.example.reminder.application.port.out.UserPersistencePort;
import com.example.reminder.infrastructure.persistence.entity.User;
import com.example.reminder.domain.model.UserModel;
import com.example.reminder.application.exception.ResourceNotFoundException;
import com.example.reminder.infrastructure.persistence.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserPersistencePort {

    private final UserRepository userRepository;

    @Override
    public List<UserModel> findAllActive() {
        return userRepository.findAllByDeletedAtIsNull().stream().map(this::toModel).toList();
    }

    @Override
    public Optional<UserModel> findActiveById(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id).map(this::toModel);
    }

    @Override
    public boolean existsActiveByEmail(String email) {
        return userRepository.existsByEmailAndDeletedAtIsNull(email);
    }

    @Override
    public UserModel save(UserModel userModel) {
        User user = userModel.id() == null
                ? new User()
                : userRepository.findById(userModel.id())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userModel.id()));

        user.setEmail(userModel.email());
        user.setPasswordHash(userModel.passwordHash());
        user.setFullName(userModel.fullName());
        user.setTonePreference(userModel.tonePreference());
        user.setStatus(userModel.status());
        user.setCreatedAt(userModel.createdAt());
        user.setDeletedAt(userModel.deletedAt());

        return toModel(userRepository.save(user));
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




