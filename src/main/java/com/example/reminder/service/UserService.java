package com.example.reminder.service;

import com.example.reminder.dto.user.CreateUserCommand;
import com.example.reminder.dto.user.UpdateUserCommand;
import com.example.reminder.domain.model.UserModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    Page<UserModel> findAll(Pageable pageable);

    UserModel findById(Long id);

    UserModel create(CreateUserCommand command);

    UserModel update(Long id, UpdateUserCommand command);

    void delete(Long id);

    UserModel getActiveUser(Long id);
}
