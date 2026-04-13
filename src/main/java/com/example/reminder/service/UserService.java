package com.example.reminder.service;

import com.example.reminder.dto.user.CreateUserCommand;
import com.example.reminder.dto.user.UpdateUserCommand;
import com.example.reminder.domain.model.UserModel;
import java.util.List;

public interface UserService {

    List<UserModel> findAll();

    UserModel findById(Long id);

    UserModel create(CreateUserCommand command);

    UserModel update(Long id, UpdateUserCommand command);

    void delete(Long id);

    UserModel getActiveUser(Long id);
}
