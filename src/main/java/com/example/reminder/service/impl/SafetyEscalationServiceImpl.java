package com.example.reminder.service.impl;

import com.example.reminder.domain.enums.ActivityLogType;
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
import com.example.reminder.service.ActivityLogService;
import com.example.reminder.service.EmailService;
import com.example.reminder.service.NotificationService;
import com.example.reminder.service.PassiveActivityService;
import com.example.reminder.service.SafetyEscalationService;
import com.example.reminder.service.SmsService;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
    private static final int PASSIVE_ACTIVITY_DELAY_LEVEL = -1;
    private static final long TRUSTED_CONTACT_DELAY_MINUTES = 5;
    private static final long RECENT_APP_ACTIVITY_MINUTES = 10;

    private final ReminderInstanceRepository reminderInstanceRepository;
    private final TrustedContactRepository trustedContactRepository;
    private final EscalationLogRepository escalationLogRepository;
    private final SafetyEventRepository safetyEventRepository;
    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final EmailService emailService;
    private final SmsService smsService;
    private final PassiveActivityService passiveActivityService;

    @Override
    @Transactional
    public void processDueEscalations() {
        LocalDateTime now = LocalDateTime.now();
        List<ReminderInstance> instances = new ArrayList<>(reminderInstanceRepository.findDueSafetyInstances(
                ReminderStatus.ACTIVE,
                ReminderSourceType.SYSTEM,
                List.of(ReminderInstanceStatus.PENDING, ReminderInstanceStatus.SNOOZED, ReminderInstanceStatus.ESCALATED),
                now
        ));
        instances.addAll(reminderInstanceRepository.findDueSafetyInstances(
                ReminderStatus.ACTIVE,
                ReminderSourceType.USER,
                List.of(ReminderInstanceStatus.PENDING, ReminderInstanceStatus.SNOOZED, ReminderInstanceStatus.ESCALATED),
                now
        ));

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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + userId));
        List<TrustedContact> contacts = trustedContactRepository
                .findByUserIdAndDeletedAtIsNullAndIsActiveTrueOrderByPriorityAscCreatedAtAsc(userId);
        for (TrustedContact contact : contacts) {
            if (!hasEmail(contact) && !hasPhone(contact)) {
                log.info("Trusted contact {} has no email or phone, skipping SOS alert", contact.getId());
                continue;
            }
            try {
                if (hasEmail(contact)) {
                    sendTrustedContactEmail(contact, user, 7, "Người dùng đã nhấn SOS", true, false);
                }
                if (hasPhone(contact)) {
                    sendTrustedContactSms(contact, user, 7, "Người dùng đã nhấn SOS", true);
                }
            } catch (RuntimeException ex) {
                log.error("Failed to send SOS alert to trusted contact {} for user {}", contact.getId(), userId, ex);
            }
        }
        activityLogService.record(
                user.getId(),
                ActivityLogType.SOS_SENT,
                "Đã gửi SOS",
                "Bạn đã gửi cảnh báo SOS tới người liên hệ tin cậy.",
                null,
                null,
                null,
                null
        );
    }

    private void processInstance(ReminderInstance instance, LocalDateTime now) {
        if (instance.getReminder().getSourceType() == ReminderSourceType.USER) {
            processUserSafetyInstance(instance, now);
            return;
        }

        long elapsedMinutes = ChronoUnit.MINUTES.between(instance.getScheduledTime(), now);
        int targetLevel = resolveSystemTargetLevel(instance, elapsedMinutes);
        if (targetLevel < 0) {
            return;
        }

        if (targetLevel > 1 && delayTrustedContactAlertIfRecentlyActive(instance, targetLevel, now)) {
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

    private int resolveSystemTargetLevel(ReminderInstance instance, long elapsedMinutes) {
        if (elapsedMinutes >= LEVEL_MINUTES[FINAL_ESCALATION_LEVEL]) {
            return isLevelAlreadySent(instance, FINAL_ESCALATION_LEVEL) ? -1 : FINAL_ESCALATION_LEVEL;
        }
        return resolveNextDueUnsentLevel(instance, elapsedMinutes);
    }

    private void processUserSafetyInstance(ReminderInstance instance, LocalDateTime now) {
        int currentLevel = instance.getEscalationLevel() == null ? 0 : instance.getEscalationLevel();
        if (currentLevel < 1) {
            int reminderLevel = 1;
            try {
                sendUserNotification(instance, reminderLevel);
            } catch (RuntimeException ex) {
                log.error("Failed to send safety reminder to user {} for reminder instance {}",
                        instance.getReminder().getUser().getId(), instance.getId(), ex);
            }
            saveEscalationLog(instance, reminderLevel, NotificationType.SOUND);
            instance.setStatus(ReminderInstanceStatus.ESCALATED);
            instance.setEscalationLevel(reminderLevel);
            instance.setLastNotificationAt(now);
            instance.setNextRemindAt(now.plusMinutes(15));
            reminderInstanceRepository.save(instance);
            return;
        }

        if (currentLevel >= FINAL_ESCALATION_LEVEL) {
            return;
        }

        if (delayTrustedContactAlertIfRecentlyActive(instance, FINAL_ESCALATION_LEVEL, now)) {
            return;
        }

        try {
            sendUserNotification(instance, FINAL_ESCALATION_LEVEL);
        } catch (RuntimeException ex) {
            log.error("Failed to send final safety notification to user {} for reminder instance {}",
                instance.getReminder().getUser().getId(), instance.getId(), ex);
        }
        sendTrustedContactForLevel(instance, FINAL_ESCALATION_LEVEL);
        saveEscalationLog(instance, FINAL_ESCALATION_LEVEL, NotificationType.EMAIL);
        instance.setStatus(ReminderInstanceStatus.MISSED);
        instance.setEscalationLevel(FINAL_ESCALATION_LEVEL);
        instance.setMissedCount((instance.getMissedCount() == null ? 0 : instance.getMissedCount()) + 1);
        instance.setLastNotificationAt(now);
        instance.setNextRemindAt(null);
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
            case 0 -> "Check-in hằng ngày";
            case 1 -> "Nhắc lại check-in hằng ngày";
            default -> "Cảnh báo an toàn cấp " + level;
        };
        String body = switch (level) {
            case 0 -> "Vui lòng xác nhận bạn vẫn an toàn.";
            case 1 -> "Bạn đã bỏ lỡ check-in. Vui lòng xác nhận bạn vẫn an toàn.";
            default -> "Bạn vẫn chưa phản hồi check-in an toàn. Chúng tôi đã cảnh báo người liên hệ tin cậy của bạn. Vui lòng xác nhận bạn vẫn an toàn.";
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

    private boolean delayTrustedContactAlertIfRecentlyActive(ReminderInstance instance, int targetLevel, LocalDateTime now) {
        Long userId = instance.getReminder().getUser().getId();
        if (escalationLogRepository.existsByReminderInstanceIdAndNotificationTypeAndDeletedAtIsNull(
                instance.getId(),
                NotificationType.PASSIVE_ACTIVITY_DELAY
        )) {
            return false;
        }

        return passiveActivityService
                .findRecentStrongActivity(userId, instance.getScheduledTime(), now, RECENT_APP_ACTIVITY_MINUTES)
                .map(evidence -> {
                    sendPassiveActivityFinalPrompt(instance, targetLevel, evidence);
                    saveEscalationLog(instance, PASSIVE_ACTIVITY_DELAY_LEVEL, NotificationType.PASSIVE_ACTIVITY_DELAY);
                    instance.setStatus(ReminderInstanceStatus.ESCALATED);
                    instance.setLastNotificationAt(now);
                    instance.setNextRemindAt(now.plusMinutes(TRUSTED_CONTACT_DELAY_MINUTES));
                    reminderInstanceRepository.save(instance);
                    return true;
                })
                .orElse(false);
    }

    private void sendPassiveActivityFinalPrompt(
            ReminderInstance instance,
            int targetLevel,
            PassiveActivityService.RecentActivityEvidence evidence
    ) {
        try {
            notificationService.send(new SendNotificationRequest(
                    instance.getReminder().getUser().getId(),
                    "Vui long check-in ngay",
                    "AfterMe thay ban vua hoat dong gan day. Vui long xac nhan an toan de tranh gui canh bao cho nguoi than.",
                    instance.getReminder().getId(),
                    instance.getSchedule() == null ? null : instance.getSchedule().getId(),
                    instance.getId(),
                    instance.getReminder().getSourceType(),
                    true
            ));
        } catch (RuntimeException ex) {
            log.error("Failed to send passive activity final prompt to user {} for reminder instance {} at level {}",
                    instance.getReminder().getUser().getId(), instance.getId(), targetLevel, ex);
        }
        activityLogService.record(
                instance.getReminder().getUser().getId(),
                ActivityLogType.ESCALATION_TRIGGERED,
                "Tam hoan canh bao nguoi than",
                "AfterMe da phat hien hoat dong gan day tren ung dung va tam hoan canh bao nguoi than 5 phut de ban kip check-in.",
                instance.getReminder().getId(),
                instance.getSchedule() == null ? null : instance.getSchedule().getId(),
                instance.getId(),
                "{\"passiveActivityDelay\":true,\"activityType\":\"" + evidence.signalType()
                        + "\",\"activityAt\":\"" + evidence.occurredAt() + "\"}"
        );
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
            sendAndRecordEmail(instance, levelContact, level, "Người dùng chưa phản hồi check-in hằng ngày", finalLevel);
        } else if (!finalLevel && hasPhone(levelContact)) {
            sendAndRecordSms(instance, levelContact, level, "Người dùng chưa phản hồi check-in hằng ngày", false);
        } else if (!hasPhone(levelContact)) {
            recordFailedSafetyEvent(instance, levelContact, SafetyMethod.EMAIL,
                    "Trusted contact has no email for safety alert");
        }

        if (finalLevel) {
            TrustedContact highestPriorityContact = contacts.get(0);
            if (hasPhone(highestPriorityContact)) {
                sendAndRecordSms(instance, highestPriorityContact, level, "Người dùng chưa phản hồi check-in hằng ngày", true);
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
            sendTrustedContactEmail(contact, instance.getReminder().getUser(), level, reason, includeLocation, isNightAlert(instance));
            event.setStatus(SafetyEventStatus.SENT);
        } catch (RuntimeException ex) {
            event.setStatus(SafetyEventStatus.FAILED);
            log.error("Failed to send safety alert email to trusted contact {} for user {} at level {}",
                    contact.getId(), instance.getReminder().getUser().getId(), level, ex);
        }
        SafetyEvent saved = safetyEventRepository.save(event);
        recordSafetyEventActivity(saved, level, includeLocation);
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
        SafetyEvent saved = safetyEventRepository.save(event);
        recordSafetyEventActivity(saved, level, includeLocation);
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
        SafetyEvent saved = safetyEventRepository.save(event);
        recordSafetyEventActivity(saved, instance.getEscalationLevel(), false);
        log.info("{}: contactId={}, userId={}, level={}",
                reason,
                contact.getId(),
                instance.getReminder().getUser().getId(),
                instance.getEscalationLevel());
    }

    private void sendTrustedContactEmail(TrustedContact contact, User user, int level, String reason, boolean includeLocation, boolean nightAlert) {
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
                buildSafetyAlertSubject(user.getFullName(), nightAlert),
                buildSafetyAlertHtml(contact.getFullName(), user.getFullName(), level, reason, locationDevice, includeLocation, nightAlert)
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
        String userName = userFullName == null || userFullName.isBlank() ? "người dùng AfterMe" : userFullName;
        String message = "Cảnh báo AfterMe cấp " + level + ": " + userName
                + " chưa phản hồi. Lý do: " + reason + ". Vui lòng liên hệ ngay.";
        if (!includeLocation) {
            return message;
        }
        if (locationDevice == null) {
            return message + " Vị trí gần nhất: không có dữ liệu.";
        }
        return message + " Vị trí: https://www.google.com/maps?q="
                + locationDevice.getLastLatitude() + "," + locationDevice.getLastLongitude();
    }

    private String buildSafetyAlertHtml(
            String contactName,
            String userFullName,
            int level,
            String reason,
            UserDevice locationDevice,
            boolean includeLocation,
            boolean nightAlert
    ) {
        String locationHtml = buildLocationHtml(locationDevice, includeLocation);
        String nightDisclaimerHtml = buildNightAlertDisclaimerHtml(nightAlert);
        return String.format("""
                <!doctype html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                </head>
                <body style="margin:0;padding:0;background:#fff5f5;font-family:Arial,Helvetica,sans-serif;color:#7f1d1d;">
                    <div style="max-width:640px;margin:0 auto;padding:32px 16px;">
                        <div style="background:#ffffff;border:1px solid #fecaca;border-radius:18px;overflow:hidden;">
                            <div style="background:#dc2626;padding:28px 32px;color:#ffffff;">
                                <div style="font-size:14px;letter-spacing:1.8px;text-transform:uppercase;opacity:0.9;">Cảnh báo an toàn AfterMe</div>
                                <h1 style="margin:8px 0 0;font-size:28px;line-height:1.2;">Cần kiểm tra ngay</h1>
                            </div>
                            <div style="padding:32px;">
                                <p style="margin:0 0 16px;font-size:16px;line-height:1.7;">Xin chào %s,</p>
                                <p style="margin:0 0 16px;font-size:16px;line-height:1.7;">%s chưa phản hồi check-in an toàn.</p>
                                <div style="margin:24px 0;padding:16px 18px;border-left:4px solid #dc2626;background:#fef2f2;border-radius:12px;">
                                    <p style="margin:0 0 8px;font-size:14px;line-height:1.6;color:#991b1b;"><strong>Cấp cảnh báo:</strong> %d</p>
                                    <p style="margin:0;font-size:14px;line-height:1.6;color:#991b1b;"><strong>Lý do:</strong> %s</p>
                                </div>
                                %s
                                %s
                                <p style="margin:16px 0 0;font-size:14px;line-height:1.6;color:#991b1b;">Vui lòng liên hệ trực tiếp hoặc thực hiện hành động an toàn phù hợp.</p>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """,
                contactName == null || contactName.isBlank() ? "bạn" : contactName,
                userFullName,
                level,
                reason,
                nightDisclaimerHtml,
                locationHtml
        );
    }

    private String buildSafetyAlertSubject(String userFullName, boolean nightAlert) {
        String userName = userFullName == null || userFullName.isBlank() ? "nguoi dung AfterMe" : userFullName;
        String prefix = nightAlert ? "[CANH BAO BAN DEM] " : "[CANH BAO AN TOAN] ";
        return prefix + "AfterMe: " + userName + " chua phan hoi check-in";
    }

    private String buildNightAlertDisclaimerHtml(boolean nightAlert) {
        if (!nightAlert) {
            return "";
        }
        return """
                <div style="margin:24px 0;padding:16px 18px;border-left:4px solid #7c3aed;background:#f5f3ff;border-radius:12px;">
                    <p style="margin:0 0 8px;font-size:14px;line-height:1.6;color:#5b21b6;"><strong>Luu y canh bao ban dem:</strong></p>
                    <p style="margin:0;font-size:14px;line-height:1.6;color:#5b21b6;">Email nay co the duoc gui vao ban dem. Email co the khong duoc doc ngay neu nguoi nhan dang ngu hoac tat thong bao. Neu ban thay email nay, vui long lien he nguoi dung som nhat co the.</p>
                </div>
                """;
    }

    private boolean isNightAlert(ReminderInstance instance) {
        LocalDateTime now = LocalDateTime.now();
        if (isQuietHours(now.toLocalTime())) {
            return true;
        }
        return instance.getResponseDeadline() != null
                && isQuietHours(instance.getResponseDeadline().toLocalTime());
    }

    private boolean isQuietHours(LocalTime time) {
        return !time.isBefore(LocalTime.MIDNIGHT) && time.isBefore(LocalTime.of(6, 0));
    }

    private String buildLocationHtml(UserDevice device, boolean includeLocation) {
        if (!includeLocation) {
            return "";
        }
        if (device == null) {
            return """
                    <div style="margin:24px 0;padding:16px 18px;border-left:4px solid #f97316;background:#fff7ed;border-radius:12px;">
                        <p style="margin:0;font-size:14px;line-height:1.6;color:#9a3412;"><strong>Vị trí gần nhất:</strong> Không có dữ liệu vị trí gần đây.</p>
                    </div>
                    """;
        }

        String mapUrl = "https://www.google.com/maps?q=" + device.getLastLatitude() + "," + device.getLastLongitude();
        String accuracy = device.getLastLocationAccuracyMeters() == null
                ? "Không rõ"
                : device.getLastLocationAccuracyMeters() + " mét";
        String capturedAt = device.getLastLocationAt() == null ? "Không rõ" : device.getLastLocationAt().toString();
        String source = device.getLastLocationSource() == null ? "Không rõ" : device.getLastLocationSource();
        boolean outdated = device.getLastLocationAt() != null && device.getLastLocationAt().isBefore(LocalDateTime.now().minusHours(24));
        String outdatedLine = outdated
                ? "<p style=\"margin:8px 0 0;font-size:13px;line-height:1.6;color:#9a3412;\"><strong>Lưu ý:</strong> Vị trí này có thể đã cũ.</p>"
                : "";

        return String.format("""
                <div style="margin:24px 0;padding:16px 18px;border-left:4px solid #2563eb;background:#eff6ff;border-radius:12px;">
                    <p style="margin:0 0 8px;font-size:14px;line-height:1.6;color:#1e40af;"><strong>Vị trí gần nhất:</strong></p>
                    <p style="margin:0 0 8px;font-size:14px;line-height:1.6;color:#1e40af;"><a href="%s" style="color:#1d4ed8;">Mở trong Google Maps</a></p>
                    <p style="margin:0 0 6px;font-size:13px;line-height:1.6;color:#1e40af;"><strong>Thiết bị:</strong> %s</p>
                    <p style="margin:0 0 6px;font-size:13px;line-height:1.6;color:#1e40af;"><strong>Ghi nhận lúc:</strong> %s</p>
                    <p style="margin:0 0 6px;font-size:13px;line-height:1.6;color:#1e40af;"><strong>Độ chính xác:</strong> %s</p>
                    <p style="margin:0;font-size:13px;line-height:1.6;color:#1e40af;"><strong>Nguồn:</strong> %s</p>
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
        activityLogService.record(
                instance.getReminder().getUser().getId(),
                ActivityLogType.ESCALATION_TRIGGERED,
                "Kích hoạt cảnh báo an toàn cấp " + level,
                "Cảnh báo an toàn cấp " + level + " đã được kích hoạt cho \"" + instance.getReminder().getTitle() + "\".",
                instance.getReminder().getId(),
                instance.getSchedule() == null ? null : instance.getSchedule().getId(),
                instance.getId(),
                "{\"level\":" + level + ",\"notificationType\":\"" + notificationType + "\"}"
        );
    }

    private void recordSafetyEventActivity(SafetyEvent event, Integer level, boolean includeLocation) {
        if (event.getStatus() != SafetyEventStatus.SENT) {
            return;
        }
        String locationUrl = resolveLocationUrl(event.getUser(), includeLocation);
        activityLogService.record(
                event.getUser().getId(),
                ActivityLogType.SAFETY_ALERT_SENT,
                "Đã gửi cảnh báo an toàn",
                "Cảnh báo an toàn đã được gửi tới người liên hệ tin cậy \"" + event.getTrustedContact().getFullName() + "\"."
                        + (locationUrl == null ? "" : " Cảnh báo có kèm vị trí gần nhất."),
                event.getReminderInstance().getReminder().getId(),
                event.getReminderInstance().getSchedule() == null ? null : event.getReminderInstance().getSchedule().getId(),
                event.getReminderInstance().getId(),
                "{\"level\":" + level
                        + ",\"method\":\"" + event.getMethod() + "\""
                        + ",\"locationUrl\":" + (locationUrl == null ? "null" : "\"" + locationUrl + "\"")
                        + "}"
        );
    }

    private String resolveLocationUrl(User user, boolean includeLocation) {
        if (!includeLocation) {
            return null;
        }
        return userDeviceRepository
                .findFirstByUser_IdAndLastLatitudeIsNotNullAndLastLongitudeIsNotNullAndLastLocationAtIsNotNullOrderByLastLocationAtDescLastSeenAtDesc(user.getId())
                .map(device -> "https://www.google.com/maps?q=" + device.getLastLatitude() + "," + device.getLastLongitude())
                .orElse(null);
    }

    private boolean hasEmail(TrustedContact contact) {
        return contact.getEmail() != null && !contact.getEmail().isBlank();
    }

    private boolean hasPhone(TrustedContact contact) {
        return contact.getPhone() != null && !contact.getPhone().isBlank();
    }
}
