package com.example.reminder.controller;

import com.example.reminder.dto.notification.SendNotificationRequest;
import com.example.reminder.service.NotificationService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> send(@Valid @RequestBody SendNotificationRequest request) {
        notificationService.send(request);
        return Map.of("message", "Notification dispatched");
    }
}




