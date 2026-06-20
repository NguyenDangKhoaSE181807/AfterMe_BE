package com.example.reminder.service.impl;

import com.example.reminder.domain.enums.ActivityLogType;
import com.example.reminder.domain.enums.ReminderInstanceStatus;
import com.example.reminder.domain.enums.ReminderSourceType;
import com.example.reminder.domain.enums.ReminderStatus;
import com.example.reminder.domain.enums.ScheduleType;
import com.example.reminder.domain.enums.TonePreference;
import com.example.reminder.dto.reminder.DailyCheckInTimeUpdateResponseDto;
import com.example.reminder.entity.Reminder;
import com.example.reminder.entity.ReminderInstance;
import com.example.reminder.entity.ReminderSchedule;
import com.example.reminder.entity.User;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.repository.ReminderInstanceRepository;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.ReminderScheduleRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.ActivityLogService;
import com.example.reminder.service.DailyReminderService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyReminderServiceImpl implements DailyReminderService {

    private static final Duration MIN_CHECK_IN_INTERVAL = Duration.ofHours(8);
    private static final Duration MAX_CHECK_IN_GAP = Duration.ofHours(28);
    private static final Duration RESPONSE_WINDOW = Duration.ofHours(3);

    private final ReminderRepository reminderRepository;
    private final ReminderScheduleRepository reminderScheduleRepository;
    private final ReminderInstanceRepository reminderInstanceRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    @Override
    @Transactional
    public void createDailyCheckInReminder(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + userId));

        if (reminderRepository.findFirstByUserIdAndSourceTypeAndDeletedAtIsNullOrderByCreatedAtAsc(userId, ReminderSourceType.SYSTEM).isPresent()) {
            log.info("Lời nhắc check-in hằng ngày đã tồn tại cho người dùng: {}", userId);
            return;
        }

        LocalDateTime nextCheckInTime = nextOccurrence(user.getDailyCheckInTime());

        Reminder reminder = new Reminder();
        reminder.setUser(user);
        reminder.setTitle("Check-in hằng ngày");
        reminder.setDescription("Lời nhắc check-in hằng ngày để xác nhận bạn vẫn an toàn");
        reminder.setTone(user.getTonePreference() != null ? user.getTonePreference() : TonePreference.NORMAL);
        reminder.setSafetyEnabled(true);
        reminder.setStatus(ReminderStatus.ACTIVE);
        reminder.setSourceType(ReminderSourceType.SYSTEM);
        reminder.setCreatedAt(LocalDateTime.now());

        Reminder savedReminder = reminderRepository.save(reminder);
        log.info("Đã tạo lời nhắc check-in hằng ngày cho người dùng: {}", userId);

        ReminderSchedule schedule = new ReminderSchedule();
        schedule.setReminder(savedReminder);
        schedule.setType(ScheduleType.DAILY);
        schedule.setIntervalValue(1);
        schedule.setStartDatetime(nextCheckInTime);
        schedule.setEndDatetime(null);
        schedule.setCreatedAt(LocalDateTime.now());

        ReminderSchedule savedSchedule = reminderScheduleRepository.save(schedule);
        log.info("Đã tạo lịch check-in hằng ngày cho lời nhắc: {}", savedReminder.getId());

        ReminderInstance firstInstance = new ReminderInstance();
        firstInstance.setReminder(savedReminder);
        firstInstance.setSchedule(savedSchedule);
        firstInstance.setScheduledTime(nextCheckInTime);
        firstInstance.setResponseDeadline(nextCheckInTime.plus(RESPONSE_WINDOW));
        firstInstance.setNextRemindAt(nextCheckInTime);
        firstInstance.setStatus(ReminderInstanceStatus.PENDING);
        firstInstance.setEscalationLevel(0);
        firstInstance.setMissedCount(0);

        reminderInstanceRepository.save(firstInstance);
        log.info("Đã tạo lần nhắc check-in đầu tiên lúc: {}", nextCheckInTime);
    }

    @Override
    @Transactional
    public DailyCheckInTimeUpdateResponseDto updateDailyCheckInTime(Long userId, LocalTime checkInTime) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + userId));
        LocalTime previousTime = user.getDailyCheckInTime();
        LocalTime normalized = checkInTime == null ? LocalTime.of(20, 0) : checkInTime.withSecond(0).withNano(0);
        user.setDailyCheckInTime(normalized);
        userRepository.save(user);

        Reminder reminder = reminderRepository.findFirstByUserIdAndSourceTypeAndDeletedAtIsNullOrderByCreatedAtAsc(userId, ReminderSourceType.SYSTEM)
                .orElseGet(() -> {
                    createDailyCheckInReminder(userId);
                    return reminderRepository.findFirstByUserIdAndSourceTypeAndDeletedAtIsNullOrderByCreatedAtAsc(userId, ReminderSourceType.SYSTEM)
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời nhắc check-in hằng ngày của người dùng: " + userId));
                });

        ReminderSchedule schedule = reminderScheduleRepository.findByReminderIdAndDeletedAtIsNull(reminder.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch check-in hằng ngày của người dùng: " + userId));

        LocalDateTime now = LocalDateTime.now();
        Optional<LocalDateTime> lastSuccessfulCheckInAt = findLastSuccessfulCheckInAt(reminder.getId());
        NextCheckInPlan nextCheckInPlan = calculateNextCheckInPlan(normalized, lastSuccessfulCheckInAt.orElse(null), now);

        schedule.setStartDatetime(nextCheckInPlan.regularTime());
        schedule.setUpdatedAt(now);
        reminderScheduleRepository.save(schedule);

        reminderInstanceRepository.softDeleteFutureInstancesByReminderId(reminder.getId(), now, now);
        List<ReminderInstance> nextInstances = new ArrayList<>();
        nextCheckInPlan.transitionTime().ifPresent(transitionTime ->
                nextInstances.add(createPendingInstance(reminder, schedule, transitionTime))
        );
        nextInstances.add(createPendingInstance(reminder, schedule, nextCheckInPlan.regularTime()));
        reminderInstanceRepository.saveAll(nextInstances);

        activityLogService.record(
                user.getId(),
                ActivityLogType.DAILY_CHECK_IN_TIME_UPDATED,
                "Đã cập nhật giờ check-in",
                "Bạn đã cập nhật giờ check-in hằng ngày thành " + normalized + ".",
                reminder.getId(),
                schedule.getId(),
                nextInstances.get(0).getId(),
                "{\"previousTime\":\"" + previousTime
                        + "\",\"newTime\":\"" + normalized
                        + "\",\"nextRegularTime\":\"" + nextCheckInPlan.regularTime()
                        + "\",\"transitionTime\":\"" + nextCheckInPlan.transitionTime().map(LocalDateTime::toString).orElse(null)
                        + "\"}"
        );

        LocalDateTime expectedMissedAt = nextCheckInPlan.regularTime().plus(RESPONSE_WINDOW);
        boolean nightRisk = isNightRisk(normalized);
        return new DailyCheckInTimeUpdateResponseDto(
                normalized,
                nextCheckInPlan.regularTime(),
                nextCheckInPlan.transitionTime().orElse(null),
                expectedMissedAt,
                nightRisk,
                nightRisk ? buildNightRiskWarning(expectedMissedAt.toLocalTime()) : null
        );
    }

    private LocalDateTime nextOccurrence(LocalTime checkInTime) {
        return nextOccurrenceAfter(checkInTime, LocalDateTime.now());
    }

    private Optional<LocalDateTime> findLastSuccessfulCheckInAt(Long reminderId) {
        return reminderInstanceRepository.findLatestResolvedByReminderIdAndStatuses(
                        reminderId,
                        List.of(ReminderInstanceStatus.DONE, ReminderInstanceStatus.COMPLETED),
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst()
                .map(ReminderInstance::getResolvedAt);
    }

    private NextCheckInPlan calculateNextCheckInPlan(LocalTime checkInTime, LocalDateTime lastSuccessfulCheckInAt, LocalDateTime now) {
        LocalDateTime candidate = nextOccurrenceAfter(checkInTime, now);
        if (lastSuccessfulCheckInAt == null) {
            return new NextCheckInPlan(candidate, Optional.empty());
        }

        LocalDateTime minAllowed = lastSuccessfulCheckInAt.plus(MIN_CHECK_IN_INTERVAL);
        if (!candidate.isBefore(minAllowed)) {
            return new NextCheckInPlan(candidate, Optional.empty());
        }

        LocalDateTime nextCandidate = candidate.plusDays(1);
        Duration nextGap = Duration.between(lastSuccessfulCheckInAt, nextCandidate);
        if (nextGap.compareTo(MAX_CHECK_IN_GAP) <= 0) {
            return new NextCheckInPlan(nextCandidate, Optional.empty());
        }

        LocalDateTime transitionTime = truncateToMinute(minAllowed.isAfter(now) ? minAllowed : now);
        return new NextCheckInPlan(nextCandidate, Optional.of(transitionTime));
    }

    private LocalDateTime nextOccurrenceAfter(LocalTime checkInTime, LocalDateTime now) {
        LocalTime time = checkInTime == null ? LocalTime.of(20, 0) : checkInTime.withSecond(0).withNano(0);
        LocalDateTime candidate = now.toLocalDate().atTime(time);
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    private ReminderInstance createPendingInstance(Reminder reminder, ReminderSchedule schedule, LocalDateTime scheduledTime) {
        LocalDateTime normalizedScheduledTime = truncateToMinute(scheduledTime);
        ReminderInstance instance = new ReminderInstance();
        instance.setReminder(reminder);
        instance.setSchedule(schedule);
        instance.setScheduledTime(normalizedScheduledTime);
        instance.setResponseDeadline(normalizedScheduledTime.plus(RESPONSE_WINDOW));
        instance.setNextRemindAt(normalizedScheduledTime);
        instance.setStatus(ReminderInstanceStatus.PENDING);
        instance.setEscalationLevel(0);
        instance.setMissedCount(0);
        return instance;
    }

    private LocalDateTime truncateToMinute(LocalDateTime dateTime) {
        return dateTime.withSecond(0).withNano(0);
    }

    private boolean isNightRisk(LocalTime checkInTime) {
        LocalTime expectedMissedTime = checkInTime.plus(RESPONSE_WINDOW);
        return !expectedMissedTime.isBefore(LocalTime.MIDNIGHT)
                && expectedMissedTime.isBefore(LocalTime.of(6, 0));
    }

    private String buildNightRiskWarning(LocalTime expectedMissedTime) {
        return "Neu ban bo lo check-in, canh bao co the duoc gui cho nguoi than vao khoang "
                + expectedMissedTime
                + ". Email co the khong duoc doc ngay neu nguoi nhan dang ngu hoac tat thong bao.";
    }

    private record NextCheckInPlan(LocalDateTime regularTime, Optional<LocalDateTime> transitionTime) {
    }
}
