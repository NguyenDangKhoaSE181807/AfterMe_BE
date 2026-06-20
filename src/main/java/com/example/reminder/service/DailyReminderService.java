package com.example.reminder.service;

import com.example.reminder.dto.reminder.DailyCheckInTimeUpdateResponseDto;

/**
 * Service for managing daily check-in reminders created by the system.
 * This is a core feature where each new user automatically gets a daily reminder
 * to check in and confirm they are safe.
 */
public interface DailyReminderService {

    /**
     * Create a system daily check-in reminder for a new user.
     * Also initializes the user's safety state.
     *
     * @param userId the ID of the user to create the reminder for
     */
    void createDailyCheckInReminder(Long userId);

    DailyCheckInTimeUpdateResponseDto updateDailyCheckInTime(Long userId, java.time.LocalTime checkInTime);
}
