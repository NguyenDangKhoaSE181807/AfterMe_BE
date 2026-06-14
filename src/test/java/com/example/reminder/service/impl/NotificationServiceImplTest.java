package com.example.reminder.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.reminder.domain.enums.ActivityLogType;
import com.example.reminder.domain.enums.ReminderSourceType;
import com.example.reminder.domain.model.NotificationMessage;
import com.example.reminder.dto.notification.SendNotificationRequest;
import com.example.reminder.service.ActivityLogService;
import com.example.reminder.service.impl.notification.NotificationSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationServiceImplTest {

    private static final String DEFAULT_BODY = "M\u1edf th\u00f4ng b\u00e1o \u0111\u1ec3 xem chi ti\u1ebft nh\u1eafc nh\u1edf.";

    private final NotificationSender notificationSender = mock(NotificationSender.class);
    private final ActivityLogService activityLogService = mock(ActivityLogService.class);

    private final NotificationServiceImpl service = new NotificationServiceImpl(
            notificationSender,
            activityLogService
    );

    @Test
    void send_usesFallbackBodyWhenRequestBodyIsMissing() {
        service.send(new SendNotificationRequest(
                27L,
                "doc tin tuc",
                null,
                18L,
                24L,
                85L,
                ReminderSourceType.USER,
                false
        ));

        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationSender).send(messageCaptor.capture());
        assertEquals(DEFAULT_BODY, messageCaptor.getValue().body());

        verify(activityLogService).record(
                27L,
                ActivityLogType.NOTIFICATION_RECEIVED,
                "doc tin tuc",
                DEFAULT_BODY,
                18L,
                24L,
                85L,
                null
        );
    }
}
