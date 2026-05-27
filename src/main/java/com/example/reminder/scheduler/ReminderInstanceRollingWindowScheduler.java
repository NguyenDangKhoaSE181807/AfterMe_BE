package com.example.reminder.scheduler;

import com.example.reminder.service.ReminderInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderInstanceRollingWindowScheduler {

    private final ReminderInstanceService reminderInstanceService;

    // @Scheduled(fixedDelay = 1_800_000L)
    @Scheduled(fixedDelay = 1_800L)
    public void refreshRollingWindows() {
        try {
            reminderInstanceService.refreshRollingWindowsForActiveReminders();
        } catch (DataAccessException ex) {
            log.warn("Skipping reminder rolling-window refresh because the database is not ready yet: {}", ex.getMessage());
        }
    }
}