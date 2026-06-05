package com.example.reminder.service;

public interface SafetyEscalationService {

    void processDueEscalations();

    void sendSos(Long userId);
}
