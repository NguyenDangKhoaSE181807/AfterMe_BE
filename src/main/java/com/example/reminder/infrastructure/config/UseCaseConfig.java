package com.example.reminder.infrastructure.config;

import com.example.reminder.application.port.out.HabitPersistencePort;
import com.example.reminder.application.port.out.NotificationGateway;
import com.example.reminder.application.port.out.ReminderPersistencePort;
import com.example.reminder.application.port.out.UserPersistencePort;
import com.example.reminder.application.usecase.HabitCrudUseCase;
import com.example.reminder.application.usecase.ReminderCrudUseCase;
import com.example.reminder.application.usecase.SendNotificationUseCase;
import com.example.reminder.application.usecase.UserCrudUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public UserCrudUseCase userCrudUseCase(UserPersistencePort userPersistencePort) {
        return new UserCrudUseCase(userPersistencePort);
    }

    @Bean
    public HabitCrudUseCase habitCrudUseCase(
            HabitPersistencePort habitPersistencePort,
            UserCrudUseCase userCrudUseCase
    ) {
        return new HabitCrudUseCase(habitPersistencePort, userCrudUseCase);
    }

    @Bean
    public ReminderCrudUseCase reminderCrudUseCase(
            ReminderPersistencePort reminderPersistencePort,
            UserCrudUseCase userCrudUseCase,
            HabitCrudUseCase habitCrudUseCase
    ) {
        return new ReminderCrudUseCase(reminderPersistencePort, userCrudUseCase, habitCrudUseCase);
    }

    @Bean
    public SendNotificationUseCase sendNotificationUseCase(NotificationGateway notificationGateway) {
        return new SendNotificationUseCase(notificationGateway);
    }
}
