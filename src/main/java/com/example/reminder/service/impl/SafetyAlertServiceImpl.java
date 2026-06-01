package com.example.reminder.service.impl;

import com.example.reminder.entity.ReminderInstance;
import com.example.reminder.entity.SafetyEvent;
import com.example.reminder.domain.enums.SafetyEventStatus;
import com.example.reminder.domain.enums.SafetyMethod;
import com.example.reminder.entity.TrustedContact;
import com.example.reminder.entity.User;
import com.example.reminder.repository.SafetyEventRepository;
import com.example.reminder.repository.TrustedContactRepository;
import com.example.reminder.service.EmailService;
import com.example.reminder.service.SafetyAlertService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SafetyAlertServiceImpl implements SafetyAlertService {

    private final TrustedContactRepository trustedContactRepository;
    private final SafetyEventRepository safetyEventRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public void triggerSafetyAlert(User user, ReminderInstance instance) {
        if (instance == null) {
            throw new IllegalArgumentException("ReminderInstance is required to record safety event");
        }

        Long userId = user.getId();
        List<TrustedContact> contacts = trustedContactRepository.findByUserIdAndDeletedAtIsNull(userId);
        if (contacts.isEmpty()) {
            log.info("No trusted contacts configured for user {}", userId);
            return;
        }

        String subject = String.format("AfterMe - Safety alert for %s", user.getFullName() == null ? "your contact" : user.getFullName());
        String body = buildSafetyEmailBody(user, instance);

        for (TrustedContact tc : contacts) {
            if (!Boolean.TRUE.equals(tc.getIsActive())) {
                continue;
            }
            if (tc.getEmail() == null || tc.getEmail().isBlank()) {
                continue;
            }

            SafetyEvent ev = new SafetyEvent();
            ev.setUser(user);
            ev.setReminderInstance(instance);
            ev.setTrustedContact(tc);
            ev.setMethod(SafetyMethod.EMAIL);
            ev.setTriggeredAt(LocalDateTime.now());

            try {
                emailService.sendSafetyAlertEmail(tc.getEmail(), subject, body);
                ev.setStatus(SafetyEventStatus.SENT);
                log.info("Sent safety alert to trusted contact {} for user {}", tc.getId(), userId);
            } catch (Exception ex) {
                ev.setStatus(SafetyEventStatus.FAILED);
                log.error("Failed to send safety alert to contact {} for user {}: {}", tc.getId(), userId, ex.getMessage(), ex);
            }

            safetyEventRepository.save(ev);
        }
    }

    private String buildSafetyEmailBody(User user, ReminderInstance instance) {
        String userName = user.getFullName() == null ? "The user" : user.getFullName();
        String reminderTitle = "a reminder";
        String scheduled = "(unknown)";
        if (instance != null) {
            if (instance.getReminder() != null && instance.getReminder().getTitle() != null) {
                reminderTitle = instance.getReminder().getTitle();
            }
            if (instance.getScheduledTime() != null) {
                scheduled = instance.getScheduledTime().toString();
            }
        }

        return String.format("""
                <!doctype html>
                <html><body>
                <p>Dear trusted contact,</p>
                <p>%s may be unreachable — they missed their check-in for "%s" scheduled at %s.</p>
                <p>This message was sent automatically by AfterMe as the user reached a high risk level.</p>
                <p>If you know this user and can check on them, please do so.</p>
                <p>— AfterMe</p>
                </body></html>
                """, userName, reminderTitle, scheduled);
    }
}
