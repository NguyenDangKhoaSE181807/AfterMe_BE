package com.example.reminder.service;

import com.example.reminder.entity.ReminderInstance;
import com.example.reminder.entity.User;

public interface SafetyAlertService {

    /**
     * Trigger safety alert flow for given user and reminder instance.
     * Sends notifications to trusted contacts (email) and records SafetyEvent(s).
     */
    void triggerSafetyAlert(User user, ReminderInstance instance);

}
