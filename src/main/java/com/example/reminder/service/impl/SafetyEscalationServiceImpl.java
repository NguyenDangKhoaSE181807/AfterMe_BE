package com.example.reminder.service.impl;

import com.example.reminder.domain.enums.NotificationType;
import com.example.reminder.domain.enums.ReminderInstanceStatus;
import com.example.reminder.domain.enums.ReminderSourceType;
import com.example.reminder.domain.enums.ReminderStatus;
import com.example.reminder.domain.enums.SafetyEventStatus;
import com.example.reminder.domain.enums.SafetyMethod;
import com.example.reminder.dto.notification.SendNotificationRequest;
import com.example.reminder.entity.EscalationLog;
import com.example.reminder.entity.ReminderInstance;
import com.example.reminder.entity.SafetyEvent;
import com.example.reminder.entity.TrustedContact;
import com.example.reminder.entity.User;
import com.example.reminder.entity.UserDevice;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.repository.EscalationLogRepository;
import com.example.reminder.repository.ReminderInstanceRepository;
import com.example.reminder.repository.SafetyEventRepository;
import com.example.reminder.repository.TrustedContactRepository;
import com.example.reminder.repository.UserDeviceRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.EmailService;
import com.example.reminder.service.NotificationService;
import com.example.reminder.service.SafetyEscalationService;
import com.example.reminder.service.SmsService;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SafetyEscalationServiceImpl implements SafetyEscalationService {

    private static final int[] LEVEL_MINUTES = {0, 5, 15, 30, 60, 120, 180};
    private static final int FINAL_ESCALATION_LEVEL = LEVEL_MINUTES.length - 1;

    private final ReminderInstanceRepository reminderInstanceRepository;
    private final TrustedContactRepository trustedContactRepository;
    private final EscalationLogRepository escalationLogRepository;
    private final SafetyEventRepository safetyEventRepository;
    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final SmsService smsService;

    @Override
    @Transactional
    public void processDueEscalations() {
        LocalDateTime now = LocalDateTime.now();
        List<ReminderInstance> instances = reminderInstanceRepository.findDueSafetyInstances(
                ReminderStatus.ACTIVE,
                ReminderSourceType.SYSTEM,
                List.of(ReminderInstanceStatus.PENDING, ReminderInstanceStatus.SNOOZED, ReminderInstanceStatus.ESCALATED),
                now
        );

        for (ReminderInstance instance : instances) {
            if (instance.getResolvedAt() != null || (instance.getNextRemindAt() != null && instance.getNextRemindAt().isAfter(now))) {
                continue;
            }
            processInstance(instance, now);
        }
    }

    @Override
    @Transactional
    public void sendSos(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        List<TrustedContact> contacts = trustedContactRepository
                .findByUserIdAndDeletedAtIsNullAndIsActiveTrueOrderByPriorityAscCreatedAtAsc(userId);
        for (TrustedContact contact : contacts) {
            if (!hasEmail(contact) && !hasPhone(contact)) {
                log.info("Trusted contact {} has no email or phone, skipping SOS alert", contact.getId());
                continue;
            }
            try {
                if (hasEmail(contact)) {
                    sendTrustedContactEmail(contact, user, 7, "User pressed SOS", true);
                }
                if (hasPhone(contact)) {
                    sendTrustedContactSms(contact, user, 7, "User pressed SOS", true);
                }
            } catch (RuntimeException ex) {
                log.error("Failed to send SOS alert to trusted contact {} for user {}", contact.getId(), userId, ex);
            }
        }
    }

    private void processInstance(ReminderInstance instance, LocalDateTime now) {
        long elapsedMinutes = ChronoUnit.MINUTES.between(instance.getScheduledTime(), now);
        int targetLevel = resolveNextDueUnsentLevel(instance, elapsedMinutes);
        if (targetLevel < 0) {
            return;
        }

        try {
            sendUserNotification(instance, targetLevel);
        } catch (RuntimeException ex) {
            log.error("Failed to send app notification to user {} for reminder instance {} at level {}",
                    instance.getReminder().getUser().getId(), instance.getId(), targetLevel, ex);
        }
        if (targetLevel > 1) {
            sendTrustedContactForLevel(instance, targetLevel);
            instance.setStatus(targetLevel >= FINAL_ESCALATION_LEVEL
                    ? ReminderInstanceStatus.MISSED
                    : ReminderInstanceStatus.ESCALATED);
        }
        saveEscalationLog(instance, targetLevel, resolveNotificationType(targetLevel));

        instance.setEscalationLevel(Math.max(instance.getEscalationLevel() == null ? 0 : instance.getEscalationLevel(), targetLevel));
        instance.setLastNotificationAt(now);
        instance.setNextRemindAt(resolveNextRemindAt(instance.getScheduledTime(), targetLevel));
        reminderInstanceRepository.save(instance);
    }

    private int resolveNextDueUnsentLevel(ReminderInstance instance, long elapsedMinutes) {
        for (int i = 0; i < LEVEL_MINUTES.length; i++) {
            if (elapsedMinutes < LEVEL_MINUTES[i]) {
                return -1;
            }
            if (!isLevelAlreadySent(instance, i)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isLevelAlreadySent(ReminderInstance instance, int level) {
        if (escalationLogRepository.existsByReminderInstanceIdAndLevelAndDeletedAtIsNull(instance.getId(), level)) {
            return true;
        }

        return level == 0 && instance.getLastNotificationAt() != null;
    }

    private LocalDateTime resolveNextRemindAt(LocalDateTime scheduledTime, int currentLevel) {
        int nextLevel = currentLevel + 1;
        if (nextLevel >= LEVEL_MINUTES.length) {
            return null;
        }
        return scheduledTime.plusMinutes(LEVEL_MINUTES[nextLevel]);
    }

    private void sendUserNotification(ReminderInstance instance, int level) {
        String title = switch (level) {
            case 0 -> "Daily check-in";
            case 1 -> "Daily check-in reminder";
            default -> "Safety alert level " + level;
        };
        String body = switch (level) {
            case 0 -> "Please confirm that you are safe.";
            case 1 -> "You missed your check-in. Please confirm that you are safe.";
            default -> "Your safety check-in is still unanswered. We alerted your trusted contact. Please confirm that you are safe.";
        };
        notificationService.send(new SendNotificationRequest(
                instance.getReminder().getUser().getId(),
                title,
                body,
                instance.getReminder().getId(),
                instance.getSchedule() == null ? null : instance.getSchedule().getId(),
                instance.getId(),
                instance.getReminder().getSourceType(),
                true
        ));
    }

    private NotificationType resolveNotificationType(int level) {
        if (level == 0) {
            return NotificationType.BANNER;
        }
        if (level == 1) {
            return NotificationType.SOUND;
        }
        return NotificationType.EMAIL;
    }

    private void sendTrustedContactForLevel(ReminderInstance instance, int level) {
        User user = instance.getReminder().getUser();
        List<TrustedContact> contacts = trustedContactRepository
                .findByUserIdAndDeletedAtIsNullAndIsActiveTrueOrderByPriorityAscCreatedAtAsc(user.getId());
        if (contacts.isEmpty()) {
            log.info("No trusted contact configured for user {} at escalation level {}", user.getId(), level);
            return;
        }

        int contactIndex = level - 2;
        TrustedContact levelContact = contactIndex < contacts.size() ? contacts.get(contactIndex) : contacts.get(0);
        boolean finalLevel = level >= FINAL_ESCALATION_LEVEL;

        if (hasEmail(levelContact)) {
            sendAndRecordEmail(instance, levelContact, level, "Daily check-in was not answered", finalLevel);
        } else if (!finalLevel && hasPhone(levelContact)) {
            sendAndRecordSms(instance, levelContact, level, "Daily check-in was not answered", false);
        } else if (!hasPhone(levelContact)) {
            recordFailedSafetyEvent(instance, levelContact, SafetyMethod.EMAIL,
                    "Trusted contact has no email for safety alert");
        }

        if (finalLevel) {
            TrustedContact highestPriorityContact = contacts.get(0);
            if (hasPhone(highestPriorityContact)) {
                sendAndRecordSms(instance, highestPriorityContact, level, "Daily check-in was not answered", true);
            } else {
                recordFailedSafetyEvent(instance, highestPriorityContact, SafetyMethod.SMS,
                        "Highest priority trusted contact has no phone for level 6 SMS alert");
            }
        }
    }

    private void sendAndRecordEmail(
            ReminderInstance instance,
            TrustedContact contact,
            int level,
            String reason,
            boolean includeLocation
    ) {
        SafetyEvent event = createSafetyEvent(instance, contact, SafetyMethod.EMAIL);
        try {
            sendTrustedContactEmail(contact, instance.getReminder().getUser(), level, reason, includeLocation);
            event.setStatus(SafetyEventStatus.SENT);
        } catch (RuntimeException ex) {
            event.setStatus(SafetyEventStatus.FAILED);
            log.error("Failed to send safety alert email to trusted contact {} for user {} at level {}",
                    contact.getId(), instance.getReminder().getUser().getId(), level, ex);
        }
        safetyEventRepository.save(event);
    }

    private void sendAndRecordSms(
            ReminderInstance instance,
            TrustedContact contact,
            int level,
            String reason,
            boolean includeLocation
    ) {
        SafetyEvent event = createSafetyEvent(instance, contact, SafetyMethod.SMS);
        try {
            sendTrustedContactSms(contact, instance.getReminder().getUser(), level, reason, includeLocation);
            event.setStatus(SafetyEventStatus.SENT);
        } catch (RuntimeException ex) {
            event.setStatus(SafetyEventStatus.FAILED);
            log.error("Failed to send safety alert SMS to trusted contact {} for user {} at level {}",
                    contact.getId(), instance.getReminder().getUser().getId(), level, ex);
        }
        safetyEventRepository.save(event);
    }

    private SafetyEvent createSafetyEvent(ReminderInstance instance, TrustedContact contact, SafetyMethod method) {
        SafetyEvent event = new SafetyEvent();
        event.setUser(instance.getReminder().getUser());
        event.setReminderInstance(instance);
        event.setTrustedContact(contact);
        event.setMethod(method);
        event.setTriggeredAt(LocalDateTime.now());
        return event;
    }

    private void recordFailedSafetyEvent(
            ReminderInstance instance,
            TrustedContact contact,
            SafetyMethod method,
            String reason
    ) {
        SafetyEvent event = createSafetyEvent(instance, contact, method);
        event.setStatus(SafetyEventStatus.FAILED);
        safetyEventRepository.save(event);
        log.info("{}: contactId={}, userId={}, level={}",
                reason,
                contact.getId(),
                instance.getReminder().getUser().getId(),
                instance.getEscalationLevel());
    }

    private void sendTrustedContactEmail(TrustedContact contact, User user, int level, String reason, boolean includeLocation) {
        if (contact.getEmail() == null || contact.getEmail().isBlank()) {
            log.info("Trusted contact {} has no email, skipping safety alert", contact.getId());
            return;
        }
        UserDevice locationDevice = includeLocation
                ? userDeviceRepository.findFirstByUser_IdAndLastLatitudeIsNotNullAndLastLongitudeIsNotNullAndLastLocationAtIsNotNullOrderByLastLocationAtDescLastSeenAtDesc(user.getId())
                        .orElse(null)
                : null;
        emailService.sendSafetyAlertEmail(
                contact.getEmail(),
                "AfterMe - Safety alert for " + user.getFullName(),
                buildSafetyAlertHtml(contact.getFullName(), user.getFullName(), level, reason, locationDevice, includeLocation)
        );
    }

    private void sendTrustedContactSms(TrustedContact contact, User user, int level, String reason, boolean includeLocation) {
        if (contact.getPhone() == null || contact.getPhone().isBlank()) {
            log.info("Trusted contact {} has no phone, skipping safety SMS", contact.getId());
            return;
        }
        UserDevice locationDevice = includeLocation
                ? userDeviceRepository.findFirstByUser_IdAndLastLatitudeIsNotNullAndLastLongitudeIsNotNullAndLastLocationAtIsNotNullOrderByLastLocationAtDescLastSeenAtDesc(user.getId())
                        .orElse(null)
                : null;
        smsService.sendSafetyAlertSms(contact.getPhone(), buildSafetyAlertSms(user.getFullName(), level, reason, locationDevice, includeLocation));
    }

    private String buildSafetyAlertSms(
            String userFullName,
            int level,
            String reason,
            UserDevice locationDevice,
            boolean includeLocation
    ) {
        String userName = userFullName == null || userFullName.isBlank() ? "AfterMe user" : userFullName;
        String message = "AfterMe Alert L" + level + ": " + userName
                + " has not responded. Reason: " + reason + ". Please contact them immediately.";
        if (!includeLocation) {
            return message;
        }
        if (locationDevice == null) {
            return message + " Last known location: unavailable.";
        }
        return message + " Location: https://www.google.com/maps?q="
                + locationDevice.getLastLatitude() + "," + locationDevice.getLastLongitude();
    }

    private String buildSafetyAlertHtml(
            String contactName,
            String userFullName,
            int level,
            String reason,
            UserDevice locationDevice,
            boolean includeLocation
    ) {
        String locationHtml = buildLocationHtml(locationDevice, includeLocation);
        return String.format("""
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                </head>
                <body style="margin:0;padding:0;background:#fff5f5;font-family:Arial,Helvetica,sans-serif;color:#7f1d1d;">
                    <div style="max-width:640px;margin:0 auto;padding:32px 16px;">
                        <div style="background:#ffffff;border:1px solid #fecaca;border-radius:18px;overflow:hidden;">
                            <div style="background:#dc2626;padding:28px 32px;color:#ffffff;">
                                <div style="font-size:14px;letter-spacing:1.8px;text-transform:uppercase;opacity:0.9;">AfterMe Safety Alert</div>
                                <h1 style="margin:8px 0 0;font-size:28px;line-height:1.2;">Immediate attention needed</h1>
                            </div>
                            <div style="padding:32px;">
                                <p style="margin:0 0 16px;font-size:16px;line-height:1.7;">Hello %s,</p>
                                <p style="margin:0 0 16px;font-size:16px;line-height:1.7;">%s has not responded to a safety check-in.</p>
                                <div style="margin:24px 0;padding:16px 18px;border-left:4px solid #dc2626;background:#fef2f2;border-radius:12px;">
                                    <p style="margin:0 0 8px;font-size:14px;line-height:1.6;color:#991b1b;"><strong>Alert level:</strong> %d</p>
                                    <p style="margin:0;font-size:14px;line-height:1.6;color:#991b1b;"><strong>Reason:</strong> %s</p>
                                </div>
                                %s
                                <p style="margin:16px 0 0;font-size:14px;line-height:1.6;color:#991b1b;">Please contact them directly or take appropriate safety action.</p>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """,
                contactName == null || contactName.isBlank() ? "there" : contactName,
                userFullName,
                level,
                reason,
                locationHtml
        );
    }

    private String buildLocationHtml(UserDevice device, boolean includeLocation) {
        if (!includeLocation) {
            return "";
        }
        if (device == null) {
            return """
                    <div style="margin:24px 0;padding:16px 18px;border-left:4px solid #f97316;background:#fff7ed;border-radius:12px;">
                        <p style="margin:0;font-size:14px;line-height:1.6;color:#9a3412;"><strong>Last known location:</strong> No recent device location available.</p>
                    </div>
                    """;
        }

        String mapUrl = "https://www.google.com/maps?q=" + device.getLastLatitude() + "," + device.getLastLongitude();
        String accuracy = device.getLastLocationAccuracyMeters() == null
                ? "Unknown"
                : device.getLastLocationAccuracyMeters() + " meters";
        String capturedAt = device.getLastLocationAt() == null ? "Unknown" : device.getLastLocationAt().toString();
        String source = device.getLastLocationSource() == null ? "Unknown" : device.getLastLocationSource();
        boolean outdated = device.getLastLocationAt() != null && device.getLastLocationAt().isBefore(LocalDateTime.now().minusHours(24));
        String outdatedLine = outdated
                ? "<p style=\"margin:8px 0 0;font-size:13px;line-height:1.6;color:#9a3412;\"><strong>Note:</strong> This location may be outdated.</p>"
                : "";

        return String.format("""
                <div style="margin:24px 0;padding:16px 18px;border-left:4px solid #2563eb;background:#eff6ff;border-radius:12px;">
                    <p style="margin:0 0 8px;font-size:14px;line-height:1.6;color:#1e40af;"><strong>Last known location:</strong></p>
                    <p style="margin:0 0 8px;font-size:14px;line-height:1.6;color:#1e40af;"><a href="%s" style="color:#1d4ed8;">Open in Google Maps</a></p>
                    <p style="margin:0 0 6px;font-size:13px;line-height:1.6;color:#1e40af;"><strong>Device:</strong> %s</p>
                    <p style="margin:0 0 6px;font-size:13px;line-height:1.6;color:#1e40af;"><strong>Captured at:</strong> %s</p>
                    <p style="margin:0 0 6px;font-size:13px;line-height:1.6;color:#1e40af;"><strong>Accuracy:</strong> %s</p>
                    <p style="margin:0;font-size:13px;line-height:1.6;color:#1e40af;"><strong>Source:</strong> %s</p>
                    %s
                </div>
                """, mapUrl, device.getDeviceId(), capturedAt, accuracy, source, outdatedLine);
    }

    private void saveEscalationLog(ReminderInstance instance, int level, NotificationType notificationType) {
        EscalationLog logEntry = new EscalationLog();
        logEntry.setReminderInstance(instance);
        logEntry.setLevel(level);
        logEntry.setNotificationType(notificationType);
        logEntry.setTriggeredAt(LocalDateTime.now());
        escalationLogRepository.save(logEntry);
    }

    private boolean hasEmail(TrustedContact contact) {
        return contact.getEmail() != null && !contact.getEmail().isBlank();
    }

    private boolean hasPhone(TrustedContact contact) {
        return contact.getPhone() != null && !contact.getPhone().isBlank();
    }
}
