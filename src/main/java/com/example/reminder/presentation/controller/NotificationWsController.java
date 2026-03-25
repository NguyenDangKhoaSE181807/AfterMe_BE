package com.example.reminder.presentation.controller;

import com.example.reminder.application.dto.notification.SendNotificationRequest;
import com.example.reminder.application.usecase.SendNotificationUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class NotificationWsController {

    private final SendNotificationUseCase sendNotificationUseCase;

    @MessageMapping("/notifications.send")
    public void send(@Valid SendNotificationRequest request) {
        sendNotificationUseCase.execute(request);
    }
}




