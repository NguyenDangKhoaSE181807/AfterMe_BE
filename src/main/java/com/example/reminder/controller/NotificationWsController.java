package com.example.reminder.controller;

import com.example.reminder.dto.notification.SendNotificationRequest;
import com.example.reminder.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class NotificationWsController {

    private final NotificationService notificationService;

    @MessageMapping("/notifications.send")
    public void send(@Valid SendNotificationRequest request) {
        notificationService.send(request);
    }
}




