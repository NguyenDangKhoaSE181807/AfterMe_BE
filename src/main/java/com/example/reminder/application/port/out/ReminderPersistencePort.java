package com.example.reminder.application.port.out;

import com.example.reminder.domain.model.ReminderModel;
import java.util.List;
import java.util.Optional;

public interface ReminderPersistencePort {

    List<ReminderModel> findAllActive();

    List<ReminderModel> findAllActiveByUserId(Long userId);

    Optional<ReminderModel> findActiveById(Long id);

    ReminderModel save(ReminderModel reminder);
}




