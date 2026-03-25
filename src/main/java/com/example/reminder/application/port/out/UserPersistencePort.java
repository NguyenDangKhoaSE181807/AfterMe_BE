package com.example.reminder.application.port.out;

import com.example.reminder.domain.model.UserModel;
import java.util.List;
import java.util.Optional;

public interface UserPersistencePort {

    List<UserModel> findAllActive();

    Optional<UserModel> findActiveById(Long id);

    boolean existsActiveByEmail(String email);

    UserModel save(UserModel user);
}




