package com.example.reminder.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.reminder.domain.enums.ReminderStatus;
import com.example.reminder.domain.enums.SafetyEventStatus;
import com.example.reminder.domain.enums.SafetyMethod;
import com.example.reminder.domain.enums.TonePreference;
import com.example.reminder.domain.enums.UserRole;
import com.example.reminder.domain.enums.UserStatus;
import com.example.reminder.dto.admin.AdminDtos;
import com.example.reminder.dto.common.PagedResponseDto;
import com.example.reminder.entity.Reminder;
import com.example.reminder.entity.ReminderInstance;
import com.example.reminder.entity.SafetyEvent;
import com.example.reminder.entity.TrustedContact;
import com.example.reminder.entity.User;
import com.example.reminder.repository.AssetAccessForensicLogRepository;
import com.example.reminder.repository.AssetAccessLogRepository;
import com.example.reminder.repository.PlanRepository;
import com.example.reminder.repository.ReminderInstanceRepository;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.ActivityLogRepository;
import com.example.reminder.repository.SafetyEventRepository;
import com.example.reminder.repository.TransactionRepository;
import com.example.reminder.repository.TrustedContactRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.repository.UserResponseRepository;
import com.example.reminder.repository.UserSubscriptionRepository;
import com.example.reminder.service.EmailService;
import com.example.reminder.service.SmsService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class AdminDashboardServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserSubscriptionRepository userSubscriptionRepository = mock(UserSubscriptionRepository.class);
    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final ReminderRepository reminderRepository = mock(ReminderRepository.class);
    private final ReminderInstanceRepository reminderInstanceRepository = mock(ReminderInstanceRepository.class);
    private final PlanRepository planRepository = mock(PlanRepository.class);
    private final AssetAccessLogRepository assetAccessLogRepository = mock(AssetAccessLogRepository.class);
    private final AssetAccessForensicLogRepository assetAccessForensicLogRepository = mock(AssetAccessForensicLogRepository.class);
    private final SafetyEventRepository safetyEventRepository = mock(SafetyEventRepository.class);
    private final TrustedContactRepository trustedContactRepository = mock(TrustedContactRepository.class);
    private final ActivityLogRepository activityLogRepository = mock(ActivityLogRepository.class);
    private final UserResponseRepository userResponseRepository = mock(UserResponseRepository.class);
    private final EmailService emailService = mock(EmailService.class);
    private final SmsService smsService = mock(SmsService.class);

    private final AdminDashboardServiceImpl service = new AdminDashboardServiceImpl(
            userRepository,
            userSubscriptionRepository,
            transactionRepository,
            reminderRepository,
            reminderInstanceRepository,
            planRepository,
            assetAccessLogRepository,
            assetAccessForensicLogRepository,
            safetyEventRepository,
            trustedContactRepository,
            activityLogRepository,
            userResponseRepository,
            emailService,
            smsService
    );

    @Test
    void getReminders_usesScalarNextScheduleQuery() {
        User user = new User();
        user.setId(99L);
        user.setEmail("user@example.com");
        user.setFullName("User Example");
        user.setTonePreference(TonePreference.NORMAL);
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.CUSTOMER);

        Reminder reminder = new Reminder();
        reminder.setId(10L);
        reminder.setUser(user);
        reminder.setTitle("Daily check-in");
        reminder.setStatus(ReminderStatus.ACTIVE);
        reminder.setCreatedAt(LocalDateTime.now().minusDays(1));

        LocalDateTime nextSchedule = LocalDateTime.now().plusHours(2).withNano(0);
        when(reminderRepository.searchAdminReminders(eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(reminder)));
        when(reminderInstanceRepository.findNextScheduledTimeByReminderId(eq(10L), any(LocalDateTime.class)))
                .thenReturn(Optional.of(nextSchedule));

        PagedResponseDto<AdminDtos.ReminderRowDto> result = service.getReminders(null, null, null, 0, 10);

        assertEquals(1, result.content().size());
        AdminDtos.ReminderRowDto row = result.content().get(0);
        assertEquals(10L, row.id());
        assertEquals(99L, row.userId());
        assertEquals("user@example.com", row.userEmail());
        assertEquals(nextSchedule, row.scheduleTime());
        assertTrue(result.first());
        verify(reminderInstanceRepository).findNextScheduledTimeByReminderId(eq(10L), any(LocalDateTime.class));
        verify(reminderInstanceRepository, never()).findByReminderIdAndDeletedAtIsNull(10L);
    }

    @Test
    void resendSafetyAlert_sendsEmailAndMarksSent() {
        User user = new User();
        user.setId(99L);
        user.setEmail("user@example.com");
        user.setFullName("User Example");

        Reminder reminder = new Reminder();
        reminder.setId(10L);
        reminder.setUser(user);
        reminder.setTitle("Daily check-in");

        ReminderInstance instance = new ReminderInstance();
        instance.setId(20L);
        instance.setReminder(reminder);
        instance.setScheduledTime(LocalDateTime.now().minusHours(1));

        TrustedContact contact = new TrustedContact();
        contact.setId(30L);
        contact.setUser(user);
        contact.setFullName("Trusted Contact");
        contact.setEmail("trusted@example.com");

        SafetyEvent event = new SafetyEvent();
        event.setId(40L);
        event.setUser(user);
        event.setReminderInstance(instance);
        event.setTrustedContact(contact);
        event.setMethod(SafetyMethod.EMAIL);
        event.setStatus(SafetyEventStatus.FAILED);
        event.setTriggeredAt(LocalDateTime.now().minusMinutes(30));

        when(safetyEventRepository.findById(40L)).thenReturn(Optional.of(event));
        when(safetyEventRepository.save(any(SafetyEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminDtos.SafetyAlertRowDto result = service.resendSafetyAlert(40L);

        assertEquals(SafetyEventStatus.SENT, result.status());
        verify(emailService).sendSafetyAlertEmail(eq("trusted@example.com"), any(String.class), any(String.class));
        verify(smsService, never()).sendSafetyAlertSms(any(String.class), any(String.class));
    }
}
