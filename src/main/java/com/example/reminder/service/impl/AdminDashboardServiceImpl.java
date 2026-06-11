package com.example.reminder.service.impl;

import com.example.reminder.domain.enums.ActivityLogType;
import com.example.reminder.domain.enums.ReminderInstanceStatus;
import com.example.reminder.domain.enums.ReminderStatus;
import com.example.reminder.domain.enums.SafetyEventStatus;
import com.example.reminder.domain.enums.SafetyMethod;
import com.example.reminder.domain.enums.UserRole;
import com.example.reminder.domain.enums.UserStatus;
import com.example.reminder.dto.admin.AdminDtos;
import com.example.reminder.dto.common.PagedResponseDto;
import com.example.reminder.entity.ActivityLog;
import com.example.reminder.entity.AssetAccessForensicLog;
import com.example.reminder.entity.AssetAccessLog;
import com.example.reminder.entity.Plan;
import com.example.reminder.entity.Reminder;
import com.example.reminder.entity.ReminderInstance;
import com.example.reminder.entity.SafetyEvent;
import com.example.reminder.entity.Transaction;
import com.example.reminder.entity.TrustedContact;
import com.example.reminder.entity.User;
import com.example.reminder.entity.UserResponse;
import com.example.reminder.entity.UserSubscription;
import com.example.reminder.exception.BadRequestException;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.repository.ActivityLogRepository;
import com.example.reminder.repository.AssetAccessForensicLogRepository;
import com.example.reminder.repository.AssetAccessLogRepository;
import com.example.reminder.repository.PlanRepository;
import com.example.reminder.repository.ReminderInstanceRepository;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.SafetyEventRepository;
import com.example.reminder.repository.TransactionRepository;
import com.example.reminder.repository.TrustedContactRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.repository.UserResponseRepository;
import com.example.reminder.repository.UserSubscriptionRepository;
import com.example.reminder.service.AdminDashboardService;
import com.example.reminder.service.EmailService;
import com.example.reminder.service.SmsService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final String ACTIVE = "ACTIVE";
    private static final String PENDING = "PENDING";
    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";
    private static final String CANCELLED = "CANCELLED";
    private static final String FREEMIUM = "FREEMIUM";
    private static final String FREE = "FREE";
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final List<ReminderInstanceStatus> SUCCESSFUL_INSTANCE_STATUSES =
            List.of(ReminderInstanceStatus.DONE, ReminderInstanceStatus.COMPLETED);

    private final UserRepository userRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final TransactionRepository transactionRepository;
    private final ReminderRepository reminderRepository;
    private final ReminderInstanceRepository reminderInstanceRepository;
    private final PlanRepository planRepository;
    private final AssetAccessLogRepository assetAccessLogRepository;
    private final AssetAccessForensicLogRepository assetAccessForensicLogRepository;
    private final SafetyEventRepository safetyEventRepository;
    private final TrustedContactRepository trustedContactRepository;
    private final ActivityLogRepository activityLogRepository;
    private final UserResponseRepository userResponseRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    @Override
    @Transactional(readOnly = true)
    public AdminDtos.DashboardOverviewDto getOverview(LocalDate from, LocalDate to, String interval) {
        DateRange range = resolveRange(from, to);
        return new AdminDtos.DashboardOverviewDto(
                getOverviewCards(),
                getUserTimeseries(range.fromDate(), range.toDateInclusive(), interval),
                getSubscriptionTimeseries(range.fromDate(), range.toDateInclusive(), interval),
                getRevenueTimeseries(range.fromDate(), range.toDateInclusive(), interval),
                getChurnTimeseries(range.fromDate(), range.toDateInclusive(), interval)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDtos.DashboardCardsDto getOverviewCards() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        DateRange todayRange = new DateRange(today, today, today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        DateRange monthRange = new DateRange(
                today.withDayOfMonth(1),
                today,
                today.withDayOfMonth(1).atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );

        List<User> users = userRepository.findAll();
        List<UserSubscription> subscriptions = userSubscriptionRepository.findAll();
        return new AdminDtos.DashboardCardsDto(
                users.stream().filter(this::notDeleted).count(),
                users.stream().filter(this::notDeleted).filter(user -> user.getStatus() == UserStatus.ACTIVE).count(),
                users.stream().filter(this::premiumUser).count(),
                subscriptions.stream().filter(subscription -> isActiveSubscription(subscription, now)).count(),
                calculateMrr(subscriptions, now),
                sumSuccessfulRevenue(todayRange.fromDateTime(), todayRange.toExclusive()),
                sumSuccessfulRevenue(monthRange.fromDateTime(), monthRange.toExclusive()),
                transactionRepository.countByDeletedAtIsNullAndStatusIgnoreCase(FAILED),
                reminderInstanceRepository.countByDeletedAtIsNullAndLastNotificationAtBetween(
                        todayRange.fromDateTime(),
                        todayRange.toExclusive()
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminDtos.TimeSeriesPointDto> getUserTimeseries(LocalDate from, LocalDate to, String interval) {
        DateRange range = resolveRange(from, to);
        List<LocalDateTime> dates = userRepository.findAll().stream()
                .filter(this::notDeleted)
                .map(User::getCreatedAt)
                .filter(Objects::nonNull)
                .filter(createdAt -> !createdAt.isBefore(range.fromDateTime()) && createdAt.isBefore(range.toExclusive()))
                .toList();
        return countTimeseries(dates, range, interval);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminDtos.TimeSeriesPointDto> getSubscriptionTimeseries(LocalDate from, LocalDate to, String interval) {
        DateRange range = resolveRange(from, to);
        List<LocalDateTime> dates = userSubscriptionRepository.findAll().stream()
                .map(UserSubscription::getCreatedAt)
                .filter(Objects::nonNull)
                .filter(createdAt -> !createdAt.isBefore(range.fromDateTime()) && createdAt.isBefore(range.toExclusive()))
                .toList();
        return countTimeseries(dates, range, interval);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminDtos.TimeSeriesPointDto> getRevenueTimeseries(LocalDate from, LocalDate to, String interval) {
        DateRange range = resolveRange(from, to);
        Map<String, BigDecimal> grouped = emptyAmountBuckets(range, interval);
        transactionRepository.findByDeletedAtIsNullAndStatusIgnoreCaseAndPaidAtBetweenOrderByPaidAtAsc(
                        SUCCESS,
                        range.fromDateTime(),
                        range.toExclusive()
                )
                .forEach(transaction -> grouped.merge(periodKey(transaction.getPaidAt(), interval), transaction.getAmount(), BigDecimal::add));
        return grouped.entrySet().stream()
                .map(entry -> new AdminDtos.TimeSeriesPointDto(entry.getKey(), 0, entry.getValue(), BigDecimal.ZERO))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminDtos.TimeSeriesPointDto> getChurnTimeseries(LocalDate from, LocalDate to, String interval) {
        DateRange range = resolveRange(from, to);
        List<LocalDateTime> dates = userSubscriptionRepository.findAll().stream()
                .filter(subscription -> CANCELLED.equalsIgnoreCase(subscription.getStatus()))
                .map(this::subscriptionChurnDate)
                .filter(Objects::nonNull)
                .filter(date -> !date.isBefore(range.fromDateTime()) && date.isBefore(range.toExclusive()))
                .toList();
        return countTimeseries(dates, range, interval);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<AdminDtos.AdminUserRowDto> getUsers(String q, UserStatus status, UserRole role, int page, int size) {
        Page<User> users = userRepository.searchAdminUsers(
                likePattern(q),
                status,
                role,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PagedResponseDto.from(users.map(this::toUserRow));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDtos.AdminUserDetailDto getUser(Long id) {
        User user = getUserEntity(id);
        List<Transaction> transactions = transactionRepository.findByUserIdAndDeletedAtIsNull(id);
        BigDecimal totalRevenue = transactions.stream()
                .filter(transaction -> SUCCESS.equalsIgnoreCase(transaction.getStatus()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AdminDtos.AdminUserDetailDto(
                toUserRow(user),
                userSubscriptionRepository.findByUserIdAndDeletedAtIsNull(id).size(),
                reminderRepository.findByUserIdAndDeletedAtIsNull(id).size(),
                totalRevenue
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDtos.AdminUserSummaryDto getUserSummary() {
        List<User> users = userRepository.findAll().stream().filter(this::notDeleted).toList();
        return new AdminDtos.AdminUserSummaryDto(
                users.size(),
                users.stream().filter(user -> user.getStatus() == UserStatus.ACTIVE).count(),
                users.stream().filter(user -> user.getStatus() == UserStatus.PENDING).count(),
                users.stream().filter(user -> user.getStatus() == UserStatus.SUSPENDED).count(),
                users.stream().filter(this::premiumUser).count()
        );
    }

    @Override
    @Transactional
    public AdminDtos.AdminUserRowDto updateUser(Long id, AdminDtos.AdminUserUpdateRequest request) {
        User user = getUserEntity(id);
        if (request.email() != null && !request.email().isBlank()) {
            userRepository.findByEmailAndDeletedAtIsNull(request.email())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new BadRequestException("Email already exists");
                    });
            user.setEmail(request.email().trim());
        }
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        if (request.tonePreference() != null) {
            user.setTonePreference(request.tonePreference());
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        return toUserRow(userRepository.save(user));
    }

    @Override
    @Transactional
    public AdminDtos.AdminUserRowDto updateUserStatus(Long id, UserStatus status) {
        User user = getUserEntity(id);
        user.setStatus(status);
        return toUserRow(userRepository.save(user));
    }

    @Override
    @Transactional
    public AdminDtos.AdminUserRowDto resetUserSubscription(Long id) {
        User user = getUserEntity(id);
        LocalDateTime now = LocalDateTime.now();
        userSubscriptionRepository.findByUserIdAndDeletedAtIsNullAndStatusIn(id, List.of(ACTIVE, PENDING))
                .forEach(subscription -> {
                    subscription.setStatus(CANCELLED);
                    subscription.setDeletedAt(now);
                    subscription.setUpdatedAt(now);
                    userSubscriptionRepository.save(subscription);
                });
        user.setCurrentPlan(null);
        user.setPlanExpiresAt(null);
        return toUserRow(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<AdminDtos.SubscriptionRowDto> getSubscriptions(String q, String status, Long planId, int page, int size) {
        Page<UserSubscription> subscriptions = userSubscriptionRepository.searchAdminSubscriptions(
                likePattern(q),
                lowercase(status),
                planId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PagedResponseDto.from(subscriptions.map(this::toSubscriptionRow));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDtos.SubscriptionRowDto getSubscription(Long id) {
        return toSubscriptionRow(getSubscriptionEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDtos.SubscriptionSummaryDto getSubscriptionSummary() {
        List<UserSubscription> subscriptions = userSubscriptionRepository.findAll();
        return new AdminDtos.SubscriptionSummaryDto(
                subscriptions.size(),
                countSubscriptionStatus(subscriptions, ACTIVE),
                countSubscriptionStatus(subscriptions, "TRIAL"),
                countSubscriptionStatus(subscriptions, "EXPIRED"),
                countSubscriptionStatus(subscriptions, CANCELLED),
                countSubscriptionStatus(subscriptions, PENDING),
                countSubscriptionStatus(subscriptions, FAILED)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDtos.SubscriptionAnalyticsDto getSubscriptionAnalytics(LocalDate from, LocalDate to, String interval) {
        List<UserSubscription> subscriptions = userSubscriptionRepository.findAll();
        BigDecimal total = BigDecimal.valueOf(Math.max(subscriptions.size(), 1));
        BigDecimal conversion = percentage(BigDecimal.valueOf(countSubscriptionStatus(subscriptions, ACTIVE)), total);
        BigDecimal churn = percentage(BigDecimal.valueOf(countSubscriptionStatus(subscriptions, CANCELLED)), total);
        return new AdminDtos.SubscriptionAnalyticsDto(
                getSubscriptionTimeseries(from, to, interval),
                conversion,
                churn
        );
    }

    @Override
    @Transactional
    public AdminDtos.SubscriptionRowDto changeSubscriptionPlan(Long id, AdminDtos.SubscriptionPlanChangeRequest request) {
        UserSubscription subscription = getSubscriptionEntity(id);
        Plan plan = resolveRequestedPlan(request);
        subscription.setPlan(plan);
        subscription.setUpdatedAt(LocalDateTime.now());
        syncCurrentUserPlan(subscription);
        return toSubscriptionRow(userSubscriptionRepository.save(subscription));
    }

    @Override
    @Transactional
    public AdminDtos.SubscriptionRowDto extendSubscription(Long id, int days) {
        UserSubscription subscription = getSubscriptionEntity(id);
        subscription.setEndAt(subscription.getEndAt().plusDays(days));
        subscription.setUpdatedAt(LocalDateTime.now());
        syncCurrentUserPlan(subscription);
        return toSubscriptionRow(userSubscriptionRepository.save(subscription));
    }

    @Override
    @Transactional
    public AdminDtos.SubscriptionRowDto cancelSubscription(Long id) {
        UserSubscription subscription = getSubscriptionEntity(id);
        LocalDateTime now = LocalDateTime.now();
        Plan freemium = resolveFreemiumPlan();
        subscription.setStatus(CANCELLED);
        subscription.setPlan(freemium);
        subscription.setDeletedAt(now);
        subscription.setUpdatedAt(now);
        User user = subscription.getUser();
        user.setCurrentPlan(freemium);
        user.setPlanExpiresAt(null);
        userRepository.save(user);
        return toSubscriptionRow(userSubscriptionRepository.save(subscription));
    }

    @Override
    @Transactional
    public AdminDtos.SubscriptionRowDto reactivateSubscription(Long id) {
        UserSubscription subscription = getSubscriptionEntity(id);
        LocalDateTime now = LocalDateTime.now();
        subscription.setStatus(ACTIVE);
        subscription.setDeletedAt(null);
        subscription.setStartAt(now);
        subscription.setEndAt(calculateEndDate(now, subscription.getPlan().getBillingCycle()));
        subscription.setUpdatedAt(now);
        User user = subscription.getUser();
        user.setCurrentPlan(subscription.getPlan());
        user.setPlanExpiresAt(subscription.getEndAt());
        userRepository.save(user);
        return toSubscriptionRow(userSubscriptionRepository.save(subscription));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<AdminDtos.ReminderRowDto> getReminders(String q, ReminderStatus status, Long userId, int page, int size) {
        Page<Reminder> reminders = reminderRepository.searchAdminReminders(
                likePattern(q),
                status,
                userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PagedResponseDto.from(reminders.map(this::toReminderRow));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDtos.ReminderRowDto getReminder(Long id) {
        return toReminderRow(getReminderEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDtos.ReminderSummaryDto getReminderSummary() {
        LocalDate today = LocalDate.now();
        DateRange todayRange = new DateRange(today, today, today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        long total = reminderRepository.countByDeletedAtIsNull();
        long active = reminderRepository.countByDeletedAtIsNullAndStatus(ReminderStatus.ACTIVE);
        long sentToday = reminderInstanceRepository.countByDeletedAtIsNullAndLastNotificationAtBetween(
                todayRange.fromDateTime(),
                todayRange.toExclusive()
        );
        long scheduledToday = reminderInstanceRepository.countByDeletedAtIsNullAndScheduledTimeBetween(
                todayRange.fromDateTime(),
                todayRange.toExclusive()
        );
        long successfulToday = reminderInstanceRepository.countByDeletedAtIsNullAndStatusInAndScheduledTimeBetween(
                SUCCESSFUL_INSTANCE_STATUSES,
                todayRange.fromDateTime(),
                todayRange.toExclusive()
        );
        return new AdminDtos.ReminderSummaryDto(total, active, sentToday, successRate(successfulToday, scheduledToday));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminDtos.TimeSeriesPointDto> getReminderTimeseries(LocalDate from, LocalDate to, String interval) {
        DateRange range = resolveRange(from, to);
        List<LocalDateTime> dates = reminderRepository.findCreatedBetween(range.fromDateTime(), range.toExclusive()).stream()
                .map(Reminder::getCreatedAt)
                .filter(Objects::nonNull)
                .toList();
        return countTimeseries(dates, range, interval);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDtos.ReminderExecutionStatsDto getReminderExecutionStats(LocalDate from, LocalDate to) {
        DateRange range = resolveRange(from, to);
        long total = reminderInstanceRepository.countByDeletedAtIsNull();
        long scheduled = reminderInstanceRepository.countByDeletedAtIsNullAndScheduledTimeBetween(
                range.fromDateTime(),
                range.toExclusive()
        );
        long sent = reminderInstanceRepository.countByDeletedAtIsNullAndLastNotificationAtBetween(
                range.fromDateTime(),
                range.toExclusive()
        );
        long successful = reminderInstanceRepository.countByDeletedAtIsNullAndStatusInAndScheduledTimeBetween(
                SUCCESSFUL_INSTANCE_STATUSES,
                range.fromDateTime(),
                range.toExclusive()
        );
        long missed = reminderInstanceRepository.countByDeletedAtIsNullAndStatusInAndScheduledTimeBetween(
                List.of(ReminderInstanceStatus.MISSED),
                range.fromDateTime(),
                range.toExclusive()
        );
        long escalated = reminderInstanceRepository.countByDeletedAtIsNullAndStatusInAndScheduledTimeBetween(
                List.of(ReminderInstanceStatus.ESCALATED),
                range.fromDateTime(),
                range.toExclusive()
        );
        return new AdminDtos.ReminderExecutionStatsDto(
                total,
                scheduled,
                sent,
                successful,
                missed,
                escalated,
                successRate(successful, scheduled)
        );
    }

    @Override
    @Transactional
    public AdminDtos.ReminderRowDto updateReminderStatus(Long id, ReminderStatus status) {
        Reminder reminder = getReminderEntity(id);
        reminder.setStatus(status);
        reminder.setUpdatedAt(LocalDateTime.now());
        return toReminderRow(reminderRepository.save(reminder));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDtos.FinanceSummaryDto getFinanceSummary() {
        LocalDate today = LocalDate.now();
        BigDecimal revenueToday = sumSuccessfulRevenue(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        BigDecimal revenueMtd = sumSuccessfulRevenue(today.withDayOfMonth(1).atStartOfDay(), today.plusDays(1).atStartOfDay());
        BigDecimal revenueYtd = sumSuccessfulRevenue(today.withDayOfYear(1).atStartOfDay(), today.plusDays(1).atStartOfDay());
        BigDecimal mrr = calculateMrr(userSubscriptionRepository.findAll(), LocalDateTime.now());
        return new AdminDtos.FinanceSummaryDto(revenueToday, revenueMtd, revenueYtd, mrr, mrr.multiply(BigDecimal.valueOf(12)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminDtos.RevenueByPlanDto> getRevenueByPlan(LocalDate from, LocalDate to) {
        DateRange range = resolveWideRange(from, to);
        Map<Long, List<Transaction>> byPlan = transactionRepository
                .findByDeletedAtIsNullAndStatusIgnoreCaseAndPaidAtBetweenOrderByPaidAtAsc(SUCCESS, range.fromDateTime(), range.toExclusive())
                .stream()
                .collect(Collectors.groupingBy(transaction -> transaction.getSubscription().getPlan().getId()));
        return byPlan.entrySet().stream()
                .map(entry -> {
                    Plan plan = entry.getValue().get(0).getSubscription().getPlan();
                    BigDecimal revenue = entry.getValue().stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new AdminDtos.RevenueByPlanDto(plan.getId(), plan.getName(), revenue, entry.getValue().size());
                })
                .sorted(Comparator.comparing(AdminDtos.RevenueByPlanDto::revenue).reversed())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<AdminDtos.TransactionRowDto> getTransactions(
            String q,
            String status,
            LocalDate from,
            LocalDate to,
            int page,
            int size
    ) {
        DateRange range = optionalRange(from, to);
        Page<Transaction> transactions = transactionRepository.findAll(
                transactionSearchSpec(likePattern(q), lowercase(status), range),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PagedResponseDto.from(transactions.map(this::toTransactionRow));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDtos.TransactionRowDto getTransaction(Long id) {
        Transaction transaction = transactionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
        return toTransactionRow(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDtos.TransactionSummaryDto getTransactionSummary(LocalDate from, LocalDate to) {
        DateRange range = resolveWideRange(from, to);
        List<Transaction> transactions = transactionRepository.findCreatedBetween(range.fromDateTime(), range.toExclusive());
        BigDecimal successfulRevenue = transactions.stream()
                .filter(transaction -> SUCCESS.equalsIgnoreCase(transaction.getStatus()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AdminDtos.TransactionSummaryDto(
                transactions.size(),
                countTransactionStatus(transactions, SUCCESS),
                countTransactionStatus(transactions, FAILED),
                countTransactionStatus(transactions, PENDING),
                countTransactionStatus(transactions, "REFUNDED"),
                successfulRevenue
        );
    }

    @Override
    public AdminDtos.AdminSettingsDto getSettings() {
        return new AdminDtos.AdminSettingsDto(Map.of(
                "timezone", java.time.ZoneId.systemDefault().toString(),
                "currency", "VND",
                "csvExportEnabled", true,
                "auditLogEnabled", false
        ));
    }

    @Override
    public AdminDtos.AdminSettingsDto updateSettings(AdminDtos.AdminSettingsDto request) {
        return request == null ? getSettings() : request;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDtos.ReportOverviewDto getReportsOverview() {
        return new AdminDtos.ReportOverviewDto(
                getOverviewCards(),
                getUserSummary(),
                getSubscriptionSummary(),
                getReminderSummary(),
                getFinanceSummary()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public String exportReport(String type, LocalDate from, LocalDate to) {
        String normalizedType = clean(type);
        if (normalizedType == null) {
            throw new BadRequestException("Export type is required");
        }
        DateRange range = resolveWideRange(from, to);
        return switch (normalizedType.toLowerCase(Locale.ROOT)) {
            case "users" -> exportUsers();
            case "subscriptions" -> exportSubscriptions();
            case "transactions" -> exportTransactions(range);
            case "reminders" -> exportReminders(range);
            default -> throw new BadRequestException("Unsupported export type: " + type);
        };
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<AdminDtos.ActivityLogDto> getActivityLog(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        List<AdminDtos.ActivityLogDto> logs = new ArrayList<>();
        assetAccessLogRepository.findAll().stream()
                .map(this::toActivityLog)
                .forEach(logs::add);
        assetAccessForensicLogRepository.findAll().stream()
                .map(this::toActivityLog)
                .forEach(logs::add);
        logs.sort(Comparator.comparing(AdminDtos.ActivityLogDto::createdAt).reversed());

        int total = logs.size();
        int fromIndex = Math.min(safePage * safeSize, total);
        int toIndex = Math.min(fromIndex + safeSize, total);
        List<AdminDtos.ActivityLogDto> content = logs.subList(fromIndex, toIndex);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return new PagedResponseDto<>(
                content,
                safePage,
                safeSize,
                total,
                totalPages,
                safePage == 0,
                totalPages == 0 || safePage >= totalPages - 1,
                safePage < totalPages - 1,
                safePage > 0
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<AdminDtos.CheckInRowDto> getCheckIns(String status, Long userId, LocalDate from, LocalDate to, int page, int size) {
        DateRange range = optionalRange(from, to);
        ReminderInstanceStatus parsedStatus = parseReminderInstanceStatus(status);
        List<AdminDtos.CheckInRowDto> rows = reminderInstanceRepository.findAll().stream()
                .filter(instance -> instance.getDeletedAt() == null)
                .filter(instance -> parsedStatus == null || instance.getStatus() == parsedStatus)
                .filter(instance -> userId == null || instance.getReminder().getUser().getId().equals(userId))
                .filter(instance -> inRange(instance.getScheduledTime(), range))
                .sorted(Comparator.comparing(ReminderInstance::getScheduledTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(this::toCheckInRow)
                .toList();
        return pageList(rows, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDtos.CheckInSummaryDto getCheckInSummary(LocalDate from, LocalDate to) {
        DateRange range = optionalRange(from, to);
        LocalDate today = LocalDate.now();
        DateRange todayRange = new DateRange(today, today, today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        List<ReminderInstance> instances = reminderInstanceRepository.findAll().stream()
                .filter(instance -> instance.getDeletedAt() == null)
                .filter(instance -> inRange(instance.getScheduledTime(), range))
                .toList();
        long scheduled = instances.size();
        long completed = instances.stream().filter(instance -> SUCCESSFUL_INSTANCE_STATUSES.contains(instance.getStatus())).count();
        long missed = instances.stream().filter(instance -> instance.getStatus() == ReminderInstanceStatus.MISSED).count();
        long escalated = instances.stream().filter(instance -> instance.getStatus() == ReminderInstanceStatus.ESCALATED).count();
        long checkInsToday = instances.stream().filter(instance -> inRange(instance.getScheduledTime(), todayRange)).count();
        return new AdminDtos.CheckInSummaryDto(scheduled, completed, missed, escalated, successRate(completed, scheduled), checkInsToday);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminDtos.TimeSeriesPointDto> getCheckInTimeseries(LocalDate from, LocalDate to, String interval) {
        DateRange range = resolveRange(from, to);
        List<LocalDateTime> dates = reminderInstanceRepository.findAll().stream()
                .filter(instance -> instance.getDeletedAt() == null)
                .map(ReminderInstance::getScheduledTime)
                .filter(Objects::nonNull)
                .filter(date -> inRange(date, range))
                .toList();
        return countTimeseries(dates, range, interval);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<AdminDtos.CheckInRowDto> getReminderExecutions(Long reminderId, int page, int size) {
        getReminderEntity(reminderId);
        List<AdminDtos.CheckInRowDto> rows = reminderInstanceRepository.findByReminderIdAndDeletedAtIsNull(reminderId).stream()
                .sorted(Comparator.comparing(ReminderInstance::getScheduledTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(this::toCheckInRow)
                .toList();
        return pageList(rows, page, size);
    }

    @Override
    @Transactional
    public AdminDtos.CheckInRowDto retryCheckIn(Long id) {
        ReminderInstance instance = getReminderInstanceEntity(id);
        instance.setStatus(ReminderInstanceStatus.PENDING);
        instance.setNextRemindAt(LocalDateTime.now());
        instance.setLastNotificationAt(null);
        return toCheckInRow(reminderInstanceRepository.save(instance));
    }

    @Override
    @Transactional
    public AdminDtos.CheckInRowDto updateCheckInStatus(Long id, String status) {
        ReminderInstance instance = getReminderInstanceEntity(id);
        ReminderInstanceStatus parsedStatus = parseRequiredReminderInstanceStatus(status);
        instance.setStatus(parsedStatus);
        if (SUCCESSFUL_INSTANCE_STATUSES.contains(parsedStatus)) {
            instance.setResolvedAt(LocalDateTime.now());
            instance.setNextRemindAt(null);
        }
        return toCheckInRow(reminderInstanceRepository.save(instance));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<AdminDtos.SafetyAlertRowDto> getSafetyAlerts(SafetyEventStatus status, Long userId, LocalDate from, LocalDate to, int page, int size) {
        DateRange range = optionalRange(from, to);
        List<AdminDtos.SafetyAlertRowDto> rows = safetyEventRepository.findAll().stream()
                .filter(event -> event.getDeletedAt() == null)
                .filter(event -> status == null || event.getStatus() == status)
                .filter(event -> userId == null || event.getUser().getId().equals(userId))
                .filter(event -> inRange(event.getTriggeredAt(), range))
                .sorted(Comparator.comparing(SafetyEvent::getTriggeredAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(this::toSafetyAlertRow)
                .toList();
        return pageList(rows, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDtos.SafetyAlertSummaryDto getSafetyAlertSummary(LocalDate from, LocalDate to) {
        DateRange range = optionalRange(from, to);
        LocalDate today = LocalDate.now();
        DateRange todayRange = new DateRange(today, today, today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        List<SafetyEvent> events = safetyEventRepository.findAll().stream()
                .filter(event -> event.getDeletedAt() == null)
                .filter(event -> inRange(event.getTriggeredAt(), range))
                .toList();
        long open = events.stream().filter(event -> event.getStatus() == SafetyEventStatus.SENT).count();
        long sentToday = events.stream().filter(event -> event.getStatus() == SafetyEventStatus.SENT).filter(event -> inRange(event.getTriggeredAt(), todayRange)).count();
        long failed = events.stream().filter(event -> event.getStatus() == SafetyEventStatus.FAILED).count();
        long resolved = events.stream().filter(event -> event.getStatus() == SafetyEventStatus.ACKNOWLEDGED).count();
        return new AdminDtos.SafetyAlertSummaryDto(open, sentToday, failed, resolved, BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDtos.SafetyAlertRowDto getSafetyAlert(Long id) {
        return toSafetyAlertRow(getSafetyEventEntity(id));
    }

    @Override
    @Transactional
    public AdminDtos.SafetyAlertRowDto updateSafetyAlertStatus(Long id, String status) {
        SafetyEvent event = getSafetyEventEntity(id);
        event.setStatus(parseRequiredSafetyEventStatus(status));
        return toSafetyAlertRow(safetyEventRepository.save(event));
    }

    @Override
    @Transactional
    public AdminDtos.SafetyAlertRowDto resendSafetyAlert(Long id) {
        SafetyEvent event = getSafetyEventEntity(id);
        event.setTriggeredAt(LocalDateTime.now());
        try {
            sendSafetyAlertEvent(event);
            event.setStatus(SafetyEventStatus.SENT);
        } catch (RuntimeException ex) {
            event.setStatus(SafetyEventStatus.FAILED);
        }
        return toSafetyAlertRow(safetyEventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminDtos.TrustedContactAdminDto> getUserTrustedContacts(Long userId) {
        getUserEntity(userId);
        return trustedContactRepository.findByUserIdAndDeletedAtIsNullOrderByPriorityAscCreatedAtAsc(userId).stream()
                .map(this::toTrustedContactAdminDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminDtos.AdminPlanRowDto> getAdminPlans(Boolean active, String billingCycle) {
        String normalizedCycle = lowercase(billingCycle);
        return planRepository.findAll().stream()
                .filter(plan -> active == null || Boolean.TRUE.equals(plan.getIsActive()) == active)
                .filter(plan -> normalizedCycle == null || Objects.equals(lowercase(plan.getBillingCycle()), normalizedCycle))
                .sorted(Comparator.comparing(Plan::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(this::toAdminPlanRow)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDtos.AdminPlanSummaryDto getAdminPlanSummary() {
        List<User> users = userRepository.findAll().stream().filter(this::notDeleted).toList();
        Map<String, Long> paidByPlan = users.stream()
                .filter(this::premiumUser)
                .filter(user -> user.getCurrentPlan() != null)
                .collect(Collectors.groupingBy(user -> user.getCurrentPlan().getName(), Collectors.counting()));
        String topPlan = paidByPlan.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        return new AdminDtos.AdminPlanSummaryDto(
                planRepository.countByDeletedAtIsNullAndIsActiveTrue(),
                users.stream().filter(this::premiumUser).count(),
                topPlan,
                0
        );
    }

    @Override
    @Transactional
    public AdminDtos.AdminPlanRowDto createAdminPlan(AdminDtos.AdminPlanRequest request) {
        if (planRepository.existsByNameAndDeletedAtIsNull(request.name())) {
            throw new BadRequestException("Plan name already exists");
        }
        Plan plan = new Plan();
        applyPlanRequest(plan, request);
        plan.setCreatedAt(LocalDateTime.now());
        plan.setDeletedAt(null);
        return toAdminPlanRow(planRepository.save(plan));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDtos.AdminPlanRowDto getAdminPlan(Long id) {
        return toAdminPlanRow(getPlanEntity(id));
    }

    @Override
    @Transactional
    public AdminDtos.AdminPlanRowDto updateAdminPlan(Long id, AdminDtos.AdminPlanRequest request) {
        Plan plan = getPlanEntity(id);
        applyPlanRequest(plan, request);
        return toAdminPlanRow(planRepository.save(plan));
    }

    @Override
    @Transactional
    public AdminDtos.AdminPlanRowDto createAdminPlanPrice(Long id, AdminDtos.AdminPlanPriceRequest request) {
        Plan plan = getPlanEntity(id);
        plan.setPrice(request.price());
        if (request.billingCycle() != null && !request.billingCycle().isBlank()) {
            plan.setBillingCycle(request.billingCycle().trim());
        }
        return toAdminPlanRow(planRepository.save(plan));
    }

    @Override
    @Transactional
    public AdminDtos.AdminPlanRowDto archiveAdminPlan(Long id) {
        Plan plan = getPlanEntity(id);
        plan.setIsActive(false);
        plan.setDeletedAt(LocalDateTime.now());
        return toAdminPlanRow(planRepository.save(plan));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminDtos.NotificationTemplateDto> getNotificationTemplates(String eventType, String channel, String locale) {
        return defaultNotificationTemplates().stream()
                .filter(template -> eventType == null || template.eventType().equalsIgnoreCase(eventType))
                .filter(template -> channel == null || template.channel().equalsIgnoreCase(channel))
                .filter(template -> locale == null || template.locale().equalsIgnoreCase(locale))
                .toList();
    }

    @Override
    public AdminDtos.NotificationTemplateDto createNotificationTemplate(AdminDtos.NotificationTemplateRequest request) {
        return toNotificationTemplateDto(System.currentTimeMillis(), request);
    }

    @Override
    public AdminDtos.NotificationTemplateDto updateNotificationTemplate(Long id, AdminDtos.NotificationTemplateRequest request) {
        return toNotificationTemplateDto(id, request);
    }

    @Override
    public AdminDtos.NotificationPreviewDto previewNotificationTemplate(Long id, AdminDtos.NotificationPreviewRequest request) {
        AdminDtos.NotificationTemplateDto template = defaultNotificationTemplates().stream()
                .filter(item -> item.id().equals(id))
                .findFirst()
                .orElse(defaultNotificationTemplates().get(0));
        String body = template.body();
        if (request != null && request.variables() != null) {
            for (Map.Entry<String, Object> entry : request.variables().entrySet()) {
                body = body.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
            }
        }
        return new AdminDtos.NotificationPreviewDto(template.subject(), body);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDtos.NotificationSummaryDto getNotificationSummary() {
        LocalDate today = LocalDate.now();
        DateRange todayRange = new DateRange(today, today, today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        long sentToday = notificationActivityLogs().stream().filter(log -> inRange(log.getCreatedAt(), todayRange)).count();
        return new AdminDtos.NotificationSummaryDto(sentToday, 0, defaultNotificationTemplates().size(), 3);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<AdminDtos.NotificationLogDto> getNotificationLogs(String channel, String status, Long userId, String eventType, LocalDate from, LocalDate to, int page, int size) {
        DateRange range = optionalRange(from, to);
        String normalizedStatus = lowercase(status);
        String normalizedEventType = lowercase(eventType);
        List<AdminDtos.NotificationLogDto> rows = notificationActivityLogs().stream()
                .filter(log -> userId == null || log.getUser().getId().equals(userId))
                .filter(log -> normalizedEventType == null || log.getType().name().toLowerCase(Locale.ROOT).contains(normalizedEventType))
                .filter(log -> normalizedStatus == null || "success".contains(normalizedStatus))
                .filter(log -> channel == null || inferNotificationChannel(log).equalsIgnoreCase(channel))
                .filter(log -> inRange(log.getCreatedAt(), range))
                .sorted(Comparator.comparing(ActivityLog::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(this::toNotificationLogDto)
                .toList();
        return pageList(rows, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDtos.NotificationLogDto retryNotificationLog(Long id) {
        return toNotificationLogDto(getActivityLogEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<AdminDtos.AuditLogRowDto> getAuditLogs(String actor, String action, String targetType, String status, LocalDate from, LocalDate to, int page, int size) {
        DateRange range = optionalRange(from, to);
        String actorPattern = lowercase(actor);
        String actionPattern = lowercase(action);
        String targetPattern = lowercase(targetType);
        String statusPattern = lowercase(status);
        List<AdminDtos.AuditLogRowDto> rows = activityLogRepository.findAll().stream()
                .filter(log -> log.getDeletedAt() == null)
                .filter(log -> actorPattern == null || lowercase(log.getUser().getEmail()).contains(actorPattern))
                .filter(log -> actionPattern == null || lowercase(log.getType().name()).contains(actionPattern))
                .filter(log -> targetPattern == null || auditTargetType(log).toLowerCase(Locale.ROOT).contains(targetPattern))
                .filter(log -> statusPattern == null || "success".contains(statusPattern))
                .filter(log -> inRange(log.getCreatedAt(), range))
                .sorted(Comparator.comparing(ActivityLog::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(this::toAuditLogRow)
                .toList();
        return pageList(rows, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDtos.AuditLogSummaryDto getAuditLogSummary(LocalDate from, LocalDate to) {
        DateRange range = optionalRange(from, to);
        LocalDate today = LocalDate.now();
        DateRange todayRange = new DateRange(today, today, today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        List<ActivityLog> logs = activityLogRepository.findAll().stream()
                .filter(log -> log.getDeletedAt() == null)
                .filter(log -> inRange(log.getCreatedAt(), range))
                .toList();
        long eventsToday = logs.stream().filter(log -> inRange(log.getCreatedAt(), todayRange)).count();
        long sensitive = logs.stream().filter(log -> log.getType().name().contains("PAYMENT")
                || log.getType().name().contains("SUBSCRIPTION")
                || log.getType().name().contains("SAFETY")).count();
        long exports = logs.stream().filter(log -> log.getType().name().contains("EXPORT")).count();
        return new AdminDtos.AuditLogSummaryDto(eventsToday, 0, sensitive, exports);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDtos.AuditLogRowDto getAuditLog(Long id) {
        return toAuditLogRow(getActivityLogEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public String exportAuditLogs(String actor, String action, String targetType, String status, LocalDate from, LocalDate to) {
        List<String> rows = new ArrayList<>();
        rows.add("id,actor,action,targetType,targetId,status,metadata,createdAt");
        getAuditLogs(actor, action, targetType, status, from, to, 0, Integer.MAX_VALUE).content()
                .forEach(log -> rows.add(csv(log.id(), log.actor(), log.action(), log.targetType(), log.targetId(), log.status(), log.metadata(), log.createdAt())));
        return String.join("\n", rows);
    }

    @Override
    @Transactional
    public AdminDtos.AuditLogRowDto createAuditLog(AdminDtos.AuditLogCreateRequest request) {
        User user = resolveAuditActor(request.actor());
        ActivityLog log = new ActivityLog();
        log.setUser(user);
        log.setType(ActivityLogType.NOTIFICATION_RECEIVED);
        log.setTitle(clean(request.action()) == null ? "Admin audit event" : request.action());
        log.setMessage(clean(request.metadata()) == null ? "Admin audit event recorded" : request.metadata());
        log.setMetadata(request.metadata());
        log.setCreatedAt(LocalDateTime.now());
        return toAuditLogRow(activityLogRepository.save(log));
    }

    private User getUserEntity(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private UserSubscription getSubscriptionEntity(Long id) {
        return userSubscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + id));
    }

    private Plan resolveRequestedPlan(AdminDtos.SubscriptionPlanChangeRequest request) {
        if (request == null || ((request.planName() == null || request.planName().isBlank()) && request.planId() == null)) {
            throw new BadRequestException("planName or planId is required");
        }
        if (request.planName() != null && !request.planName().isBlank()) {
            return planRepository.findByNameIgnoreCaseAndDeletedAtIsNull(request.planName().trim())
                    .filter(plan -> Boolean.TRUE.equals(plan.getIsActive()))
                    .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + request.planName()));
        }
        return planRepository.findById(request.planId())
                .filter(found -> found.getDeletedAt() == null && Boolean.TRUE.equals(found.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + request.planId()));
    }

    private Plan resolveFreemiumPlan() {
        return planRepository.findByNameIgnoreCaseAndDeletedAtIsNull(FREEMIUM)
                .or(() -> planRepository.findByNameIgnoreCaseAndDeletedAtIsNull(FREE))
                .filter(plan -> Boolean.TRUE.equals(plan.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Freemium plan not found"));
    }

    private Reminder getReminderEntity(Long id) {
        return reminderRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found: " + id));
    }

    private ReminderInstance getReminderInstanceEntity(Long id) {
        return reminderInstanceRepository.findById(id)
                .filter(instance -> instance.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Check-in execution not found: " + id));
    }

    private SafetyEvent getSafetyEventEntity(Long id) {
        return safetyEventRepository.findById(id)
                .filter(event -> event.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Safety alert not found: " + id));
    }

    private Plan getPlanEntity(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + id));
    }

    private ActivityLog getActivityLogEntity(Long id) {
        return activityLogRepository.findById(id)
                .filter(log -> log.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Activity log not found: " + id));
    }

    private AdminDtos.CheckInRowDto toCheckInRow(ReminderInstance instance) {
        Reminder reminder = instance.getReminder();
        User user = reminder.getUser();
        UserResponse latestResponse = userResponseRepository.findByReminderInstanceIdAndDeletedAtIsNull(instance.getId()).stream()
                .max(Comparator.comparing(UserResponse::getResponseTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        return new AdminDtos.CheckInRowDto(
                instance.getId(),
                user.getId(),
                user.getEmail(),
                reminder.getId(),
                reminder.getTitle(),
                instance.getSchedule() == null ? null : instance.getSchedule().getId(),
                instance.getScheduledTime(),
                instance.getResponseDeadline(),
                instance.getLastNotificationAt(),
                instance.getResolvedAt(),
                instance.getStatus(),
                instance.getEscalationLevel(),
                instance.getMissedCount(),
                latestResponse == null ? null : latestResponse.getAction().name(),
                latestResponse == null ? null : latestResponse.getResponseTime()
        );
    }

    private AdminDtos.SafetyAlertRowDto toSafetyAlertRow(SafetyEvent event) {
        ReminderInstance instance = event.getReminderInstance();
        Reminder reminder = instance.getReminder();
        User user = event.getUser();
        TrustedContact contact = event.getTrustedContact();
        return new AdminDtos.SafetyAlertRowDto(
                event.getId(),
                user.getId(),
                user.getEmail(),
                reminder.getId(),
                reminder.getTitle(),
                instance.getId(),
                contact.getId(),
                contact.getFullName(),
                event.getMethod(),
                event.getStatus(),
                event.getTriggeredAt(),
                resolveSafetyLocationUrl(event)
        );
    }

    private void sendSafetyAlertEvent(SafetyEvent event) {
        TrustedContact contact = event.getTrustedContact();
        User user = event.getUser();
        ReminderInstance instance = event.getReminderInstance();
        Reminder reminder = instance == null ? null : instance.getReminder();
        String userName = fallback(user == null ? null : user.getFullName(), "Người dùng");
        String reminderTitle = fallback(reminder == null ? null : reminder.getTitle(), "check-in");
        String scheduledTime = instance == null || instance.getScheduledTime() == null
                ? "không xác định"
                : instance.getScheduledTime().toString();
        String locationUrl = fallback(resolveSafetyLocationUrl(event), "");
        String reason = String.format("%s chưa phản hồi nhắc nhở \"%s\" lúc %s.", userName, reminderTitle, scheduledTime);

        if (event.getMethod() == SafetyMethod.SMS) {
            String phone = contact == null ? null : contact.getPhone();
            if (phone == null || phone.isBlank()) {
                throw new BadRequestException("Trusted contact does not have a phone number");
            }
            String message = locationUrl.isBlank()
                    ? "AfterMe safety alert: " + reason
                    : "AfterMe safety alert: " + reason + " Vị trí gần nhất: " + locationUrl;
            smsService.sendSafetyAlertSms(phone, message);
            return;
        }

        String email = contact == null ? null : contact.getEmail();
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Trusted contact does not have an email");
        }

        String contactName = fallback(contact == null ? null : contact.getFullName(), "trusted contact");
        String subject = "AfterMe - Cảnh báo an toàn cho " + userName;
        String body = String.format("""
                <!doctype html>
                <html><body>
                <p>Xin chào %s,</p>
                <p>%s</p>
                %s
                <p>Vui lòng kiểm tra tình trạng của người dùng nếu bạn có thể.</p>
                <p>AfterMe</p>
                </body></html>
                """,
                contactName,
                reason,
                locationUrl.isBlank() ? "" : "<p>Vị trí gần nhất: <a href=\"" + locationUrl + "\">" + locationUrl + "</a></p>");
        emailService.sendSafetyAlertEmail(email, subject, body);
    }

    private AdminDtos.TrustedContactAdminDto toTrustedContactAdminDto(TrustedContact contact) {
        return new AdminDtos.TrustedContactAdminDto(
                contact.getId(),
                contact.getUser().getId(),
                contact.getFullName(),
                contact.getRelationship(),
                contact.getPhone(),
                contact.getEmail(),
                contact.getPriority(),
                contact.getIsActive(),
                contact.getCreatedAt()
        );
    }

    private AdminDtos.AdminPlanRowDto toAdminPlanRow(Plan plan) {
        return new AdminDtos.AdminPlanRowDto(
                plan.getId(),
                plan.getName(),
                plan.getPrice(),
                plan.getBillingCycle(),
                plan.getMaxReminders(),
                plan.getMaxTrustedContacts(),
                plan.getMaxDigitalAssets(),
                plan.getFeatures(),
                plan.getIsActive(),
                plan.getCreatedAt(),
                plan.getDeletedAt()
        );
    }

    private void applyPlanRequest(Plan plan, AdminDtos.AdminPlanRequest request) {
        plan.setName(request.name().trim());
        plan.setPrice(request.price());
        plan.setBillingCycle(request.billingCycle().trim());
        plan.setMaxReminders(request.maxReminders());
        plan.setMaxTrustedContacts(request.maxTrustedContacts());
        plan.setMaxDigitalAssets(request.maxDigitalAssets());
        plan.setFeatures(request.features());
        plan.setIsActive(request.active() == null ? Boolean.TRUE : request.active());
        if (Boolean.TRUE.equals(plan.getIsActive())) {
            plan.setDeletedAt(null);
        }
    }

    private AdminDtos.NotificationTemplateDto toNotificationTemplateDto(Long id, AdminDtos.NotificationTemplateRequest request) {
        return new AdminDtos.NotificationTemplateDto(
                id,
                fallback(request.eventType(), "REMINDER_DUE"),
                fallback(request.channel(), "PUSH"),
                fallback(request.locale(), "vi-VN"),
                fallback(request.subject(), "AfterMe notification"),
                fallback(request.body(), "Bạn có một thông báo mới từ AfterMe."),
                request.variables() == null ? List.of() : request.variables(),
                request.active() == null ? Boolean.TRUE : request.active(),
                LocalDateTime.now()
        );
    }

    private AdminDtos.NotificationLogDto toNotificationLogDto(ActivityLog log) {
        return new AdminDtos.NotificationLogDto(
                log.getId(),
                log.getUser().getId(),
                log.getType().name(),
                inferNotificationChannel(log),
                "SUCCESS",
                log.getUser().getEmail(),
                log.getMessage(),
                log.getCreatedAt()
        );
    }

    private AdminDtos.AuditLogRowDto toAuditLogRow(ActivityLog log) {
        return new AdminDtos.AuditLogRowDto(
                log.getId(),
                log.getUser().getEmail(),
                log.getType().name(),
                auditTargetType(log),
                auditTargetId(log),
                "SUCCESS",
                log.getMetadata(),
                log.getCreatedAt()
        );
    }

    private AdminDtos.AdminUserRowDto toUserRow(User user) {
        Plan plan = user.getCurrentPlan();
        return new AdminDtos.AdminUserRowDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getTonePreference(),
                user.getStatus(),
                user.getRole(),
                plan == null ? null : plan.getName(),
                user.getPlanExpiresAt(),
                user.getCreatedAt()
        );
    }

    private AdminDtos.SubscriptionRowDto toSubscriptionRow(UserSubscription subscription) {
        User user = subscription.getUser();
        Plan plan = subscription.getPlan();
        return new AdminDtos.SubscriptionRowDto(
                subscription.getId(),
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                plan.getId(),
                plan.getName(),
                plan.getBillingCycle(),
                plan.getPrice(),
                subscription.getStartAt(),
                subscription.getEndAt(),
                subscription.getStatus(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt()
        );
    }

    private AdminDtos.ReminderRowDto toReminderRow(Reminder reminder) {
        User user = reminder.getUser();
        LocalDateTime scheduleTime = reminderInstanceRepository.findNextScheduledTimeByReminderId(
                        reminder.getId(),
                        LocalDateTime.now()
                )
                .orElse(null);
        return new AdminDtos.ReminderRowDto(
                reminder.getId(),
                user.getId(),
                user.getEmail(),
                reminder.getTitle(),
                scheduleTime,
                reminder.getStatus(),
                reminder.getCreatedAt(),
                reminder.getUpdatedAt()
        );
    }

    private AdminDtos.TransactionRowDto toTransactionRow(Transaction transaction) {
        User user = transaction.getUser();
        return new AdminDtos.TransactionRowDto(
                transaction.getId(),
                user.getId(),
                user.getEmail(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getPaymentMethod(),
                transaction.getStatus(),
                transaction.getTransactionRef(),
                transaction.getPaidAt(),
                transaction.getCreatedAt()
        );
    }

    private Specification<Transaction> transactionSearchSpec(String qPattern, String status, DateRange range) {
        return (transaction, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isNull(transaction.get("deletedAt")));

            if (status != null) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(transaction.get("status")), status));
            }
            if (qPattern != null) {
                var user = transaction.join("user");
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(user.get("email")), qPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(user.get("fullName")), qPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(transaction.get("transactionRef")), qPattern)
                ));
            }
            if (range != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(transaction.get("createdAt"), range.fromDateTime()));
                predicates.add(criteriaBuilder.lessThan(transaction.get("createdAt"), range.toExclusive()));
            }

            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private AdminDtos.ActivityLogDto toActivityLog(AssetAccessLog log) {
        Long assetId = log.getDigitalAsset() == null ? null : log.getDigitalAsset().getId();
        return new AdminDtos.ActivityLogDto(
                log.getId(),
                fallback(log.getAccessedBy(), "UNKNOWN"),
                log.getAction(),
                assetId == null ? "DIGITAL_ASSET" : "DIGITAL_ASSET:" + assetId,
                log.getCreatedAt()
        );
    }

    private AdminDtos.ActivityLogDto toActivityLog(AssetAccessForensicLog log) {
        String target = log.getAttemptedAssetId() == null
                ? "ATTEMPTED_ASSET"
                : "ATTEMPTED_ASSET:" + log.getAttemptedAssetId();
        return new AdminDtos.ActivityLogDto(
                log.getId(),
                fallback(log.getActorId(), "UNKNOWN"),
                log.getAction(),
                target + ":" + log.getReasonCode(),
                log.getCreatedAt()
        );
    }

    private void syncCurrentUserPlan(UserSubscription subscription) {
        if (ACTIVE.equalsIgnoreCase(subscription.getStatus()) && subscription.getDeletedAt() == null) {
            User user = subscription.getUser();
            user.setCurrentPlan(subscription.getPlan());
            user.setPlanExpiresAt(subscription.getEndAt());
            userRepository.save(user);
        }
    }

    private boolean notDeleted(User user) {
        return user.getDeletedAt() == null;
    }

    private boolean premiumUser(User user) {
        return notDeleted(user)
                && user.getCurrentPlan() != null
                && user.getPlanExpiresAt() != null
                && user.getPlanExpiresAt().isAfter(LocalDateTime.now());
    }

    private boolean isActiveSubscription(UserSubscription subscription, LocalDateTime now) {
        return subscription.getDeletedAt() == null
                && ACTIVE.equalsIgnoreCase(subscription.getStatus())
                && subscription.getEndAt().isAfter(now);
    }

    private BigDecimal calculateMrr(List<UserSubscription> subscriptions, LocalDateTime now) {
        return subscriptions.stream()
                .filter(subscription -> isActiveSubscription(subscription, now))
                .map(subscription -> monthlyAmount(subscription.getPlan()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal monthlyAmount(Plan plan) {
        BigDecimal price = plan.getPrice() == null ? BigDecimal.ZERO : plan.getPrice();
        String cycle = plan.getBillingCycle() == null ? "MONTHLY" : plan.getBillingCycle().toUpperCase(Locale.ROOT);
        return switch (cycle) {
            case "QUARTERLY" -> price.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
            case "YEARLY", "ANNUAL" -> price.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            case "LIFETIME" -> BigDecimal.ZERO;
            default -> price;
        };
    }

    private LocalDateTime calculateEndDate(LocalDateTime startDate, String billingCycle) {
        String cycle = billingCycle == null ? "MONTHLY" : billingCycle.toUpperCase(Locale.ROOT);
        return switch (cycle) {
            case "QUARTERLY" -> startDate.plusMonths(3);
            case "YEARLY", "ANNUAL" -> startDate.plusYears(1);
            case "LIFETIME" -> startDate.plusYears(100);
            default -> startDate.plusMonths(1);
        };
    }

    private BigDecimal sumSuccessfulRevenue(LocalDateTime from, LocalDateTime to) {
        return transactionRepository.sumAmountByStatusAndPaidAtBetween(SUCCESS, from, to);
    }

    private long countSubscriptionStatus(List<UserSubscription> subscriptions, String status) {
        return subscriptions.stream().filter(subscription -> status.equalsIgnoreCase(subscription.getStatus())).count();
    }

    private long countTransactionStatus(List<Transaction> transactions, String status) {
        return transactions.stream().filter(transaction -> status.equalsIgnoreCase(transaction.getStatus())).count();
    }

    private BigDecimal successRate(long successful, long total) {
        return total == 0 ? BigDecimal.ZERO : percentage(BigDecimal.valueOf(successful), BigDecimal.valueOf(total));
    }

    private BigDecimal percentage(BigDecimal value, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return value.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    private LocalDateTime subscriptionChurnDate(UserSubscription subscription) {
        if (subscription.getUpdatedAt() != null) {
            return subscription.getUpdatedAt();
        }
        if (subscription.getDeletedAt() != null) {
            return subscription.getDeletedAt();
        }
        return subscription.getCreatedAt();
    }

    private List<AdminDtos.TimeSeriesPointDto> countTimeseries(List<LocalDateTime> dates, DateRange range, String interval) {
        Map<String, Long> buckets = emptyCountBuckets(range, interval);
        dates.forEach(date -> buckets.computeIfPresent(periodKey(date, interval), (key, value) -> value + 1));
        return buckets.entrySet().stream()
                .map(entry -> new AdminDtos.TimeSeriesPointDto(entry.getKey(), entry.getValue(), BigDecimal.ZERO, BigDecimal.ZERO))
                .toList();
    }

    private Map<String, Long> emptyCountBuckets(DateRange range, String interval) {
        Map<String, Long> buckets = new LinkedHashMap<>();
        for (String key : periodKeys(range, interval)) {
            buckets.put(key, 0L);
        }
        return buckets;
    }

    private Map<String, BigDecimal> emptyAmountBuckets(DateRange range, String interval) {
        Map<String, BigDecimal> buckets = new LinkedHashMap<>();
        for (String key : periodKeys(range, interval)) {
            buckets.put(key, BigDecimal.ZERO);
        }
        return buckets;
    }

    private List<String> periodKeys(DateRange range, String interval) {
        if (isMonthly(interval)) {
            List<String> keys = new ArrayList<>();
            YearMonth current = YearMonth.from(range.fromDate());
            YearMonth end = YearMonth.from(range.toDateInclusive());
            while (!current.isAfter(end)) {
                keys.add(current.format(MONTH_FORMAT));
                current = current.plusMonths(1);
            }
            return keys;
        }

        List<String> keys = new ArrayList<>();
        LocalDate current = range.fromDate();
        while (!current.isAfter(range.toDateInclusive())) {
            keys.add(current.format(DAY_FORMAT));
            current = current.plusDays(1);
        }
        return keys;
    }

    private String periodKey(LocalDateTime dateTime, String interval) {
        return isMonthly(interval)
                ? YearMonth.from(dateTime).format(MONTH_FORMAT)
                : dateTime.toLocalDate().format(DAY_FORMAT);
    }

    private boolean isMonthly(String interval) {
        return "MONTH".equalsIgnoreCase(interval);
    }

    private DateRange resolveRange(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        if (start.isAfter(end)) {
            throw new BadRequestException("from must be before or equal to to");
        }
        return new DateRange(start, end, start.atStartOfDay(), end.plusDays(1).atStartOfDay());
    }

    private DateRange resolveWideRange(LocalDate from, LocalDate to) {
        LocalDate start = from == null ? LocalDate.of(1970, 1, 1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        if (start.isAfter(end)) {
            throw new BadRequestException("from must be before or equal to to");
        }
        return new DateRange(start, end, start.atStartOfDay(), end.plusDays(1).atStartOfDay());
    }

    private DateRange optionalRange(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return null;
        }
        return resolveWideRange(from, to);
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String lowercase(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
    }

    private String likePattern(String value) {
        String lowered = lowercase(value);
        return lowered == null ? null : "%" + lowered + "%";
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private <T> PagedResponseDto<T> pageList(List<T> rows, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int total = rows.size();
        int fromIndex = Math.min(safePage * safeSize, total);
        int toIndex = Math.min(fromIndex + safeSize, total);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return new PagedResponseDto<>(
                rows.subList(fromIndex, toIndex),
                safePage,
                safeSize,
                total,
                totalPages,
                safePage == 0,
                totalPages == 0 || safePage >= totalPages - 1,
                safePage < totalPages - 1,
                safePage > 0
        );
    }

    private boolean inRange(LocalDateTime value, DateRange range) {
        if (range == null || value == null) {
            return true;
        }
        return !value.isBefore(range.fromDateTime()) && value.isBefore(range.toExclusive());
    }

    private ReminderInstanceStatus parseReminderInstanceStatus(String status) {
        String cleaned = clean(status);
        return cleaned == null ? null : parseRequiredReminderInstanceStatus(cleaned);
    }

    private ReminderInstanceStatus parseRequiredReminderInstanceStatus(String status) {
        String normalized = clean(status);
        if (normalized == null) {
            throw new BadRequestException("status is required");
        }
        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case "RESOLVED", "DONE", "COMPLETED" -> ReminderInstanceStatus.DONE;
            case "IGNORED", "MISSED" -> ReminderInstanceStatus.MISSED;
            case "RETRIED", "PENDING" -> ReminderInstanceStatus.PENDING;
            case "ESCALATED" -> ReminderInstanceStatus.ESCALATED;
            case "SNOOZED" -> ReminderInstanceStatus.SNOOZED;
            default -> throw new BadRequestException("Unsupported check-in status: " + status);
        };
    }

    private SafetyEventStatus parseRequiredSafetyEventStatus(String status) {
        String normalized = clean(status);
        if (normalized == null) {
            throw new BadRequestException("status is required");
        }
        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case "ACKNOWLEDGED", "RESOLVED" -> SafetyEventStatus.ACKNOWLEDGED;
            case "FAILED", "IGNORED" -> SafetyEventStatus.FAILED;
            case "PENDING", "SENT" -> SafetyEventStatus.SENT;
            default -> throw new BadRequestException("Unsupported safety alert status: " + status);
        };
    }

    private List<AdminDtos.NotificationTemplateDto> defaultNotificationTemplates() {
        LocalDateTime now = LocalDateTime.now();
        return List.of(
                new AdminDtos.NotificationTemplateDto(1L, "REMINDER_DUE", "PUSH", "vi-VN", "Nhắc nhở AfterMe", "Đã đến giờ cho {{reminderTitle}}.", List.of("reminderTitle"), true, now),
                new AdminDtos.NotificationTemplateDto(2L, "SAFETY_ALERT", "EMAIL", "vi-VN", "Cảnh báo an toàn AfterMe", "{{userName}} chưa phản hồi check-in. {{locationUrl}}", List.of("userName", "locationUrl"), true, now),
                new AdminDtos.NotificationTemplateDto(3L, "PAYMENT_FAILED", "EMAIL", "vi-VN", "Thanh toán thất bại", "Thanh toán gói {{planName}} chưa thành công.", List.of("planName"), true, now),
                new AdminDtos.NotificationTemplateDto(4L, "SUBSCRIPTION_EXPIRED", "EMAIL", "vi-VN", "Gói đăng ký đã hết hạn", "Gói {{planName}} của bạn đã hết hạn.", List.of("planName"), true, now)
        );
    }

    private List<ActivityLog> notificationActivityLogs() {
        return activityLogRepository.findAll().stream()
                .filter(log -> log.getDeletedAt() == null)
                .filter(log -> Set.of(
                        ActivityLogType.NOTIFICATION_RECEIVED,
                        ActivityLogType.ALERT_RECEIVED,
                        ActivityLogType.ESCALATION_TRIGGERED,
                        ActivityLogType.SAFETY_ALERT_SENT
                ).contains(log.getType()))
                .toList();
    }

    private String inferNotificationChannel(ActivityLog log) {
        if (log.getType() == ActivityLogType.SAFETY_ALERT_SENT) {
            return "EMAIL";
        }
        if (log.getType() == ActivityLogType.ESCALATION_TRIGGERED) {
            return "PUSH";
        }
        return "IN_APP";
    }

    private String auditTargetType(ActivityLog log) {
        if (log.getReminderId() != null) {
            return "REMINDER";
        }
        if (log.getScheduleId() != null) {
            return "SCHEDULE";
        }
        if (log.getInstanceId() != null) {
            return "CHECK_IN";
        }
        return "SYSTEM";
    }

    private String auditTargetId(ActivityLog log) {
        if (log.getReminderId() != null) {
            return String.valueOf(log.getReminderId());
        }
        if (log.getScheduleId() != null) {
            return String.valueOf(log.getScheduleId());
        }
        if (log.getInstanceId() != null) {
            return String.valueOf(log.getInstanceId());
        }
        return null;
    }

    private String resolveSafetyLocationUrl(SafetyEvent event) {
        return activityLogRepository.findAll().stream()
                .filter(log -> log.getDeletedAt() == null)
                .filter(log -> log.getType() == ActivityLogType.SAFETY_ALERT_SENT)
                .filter(log -> Objects.equals(log.getInstanceId(), event.getReminderInstance().getId()))
                .map(ActivityLog::getMetadata)
                .map(this::extractLocationUrl)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String extractLocationUrl(String metadata) {
        if (metadata == null) {
            return null;
        }
        String marker = "\"locationUrl\":\"";
        int start = metadata.indexOf(marker);
        if (start < 0) {
            return null;
        }
        int valueStart = start + marker.length();
        int valueEnd = metadata.indexOf('"', valueStart);
        return valueEnd < 0 ? null : metadata.substring(valueStart, valueEnd);
    }

    private User resolveAuditActor(String actor) {
        String cleaned = clean(actor);
        if (cleaned != null) {
            return userRepository.findByEmailAndDeletedAtIsNull(cleaned)
                    .orElseGet(() -> userRepository.findAll().stream().filter(this::notDeleted).findFirst()
                            .orElseThrow(() -> new ResourceNotFoundException("No user available for audit log")));
        }
        return userRepository.findAll().stream().filter(this::notDeleted).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No user available for audit log"));
    }

    private String exportUsers() {
        List<String> rows = new ArrayList<>();
        rows.add("id,email,fullName,status,role,currentPlan,planExpiresAt,createdAt");
        userRepository.findAll().stream()
                .filter(this::notDeleted)
                .map(this::toUserRow)
                .forEach(user -> rows.add(csv(
                        user.id(),
                        user.email(),
                        user.fullName(),
                        user.status(),
                        user.role(),
                        user.currentPlanName(),
                        user.planExpiresAt(),
                        user.createdAt()
                )));
        return String.join("\n", rows);
    }

    private String exportSubscriptions() {
        List<String> rows = new ArrayList<>();
        rows.add("id,userId,userEmail,planId,planName,billingCycle,startedAt,expiresAt,status,createdAt,updatedAt");
        userSubscriptionRepository.findAll().stream()
                .map(this::toSubscriptionRow)
                .forEach(subscription -> rows.add(csv(
                        subscription.id(),
                        subscription.userId(),
                        subscription.userEmail(),
                        subscription.planId(),
                        subscription.planName(),
                        subscription.billingCycle(),
                        subscription.startedAt(),
                        subscription.expiresAt(),
                        subscription.status(),
                        subscription.createdAt(),
                        subscription.updatedAt()
                )));
        return String.join("\n", rows);
    }

    private String exportTransactions(DateRange range) {
        List<String> rows = new ArrayList<>();
        rows.add("id,userId,userEmail,amount,currency,provider,status,transactionRef,paidAt,createdAt");
        transactionRepository.findCreatedBetween(range.fromDateTime(), range.toExclusive()).stream()
                .map(this::toTransactionRow)
                .forEach(transaction -> rows.add(csv(
                        transaction.id(),
                        transaction.userId(),
                        transaction.userEmail(),
                        transaction.amount(),
                        transaction.currency(),
                        transaction.provider(),
                        transaction.status(),
                        transaction.transactionRef(),
                        transaction.paidAt(),
                        transaction.createdAt()
                )));
        return String.join("\n", rows);
    }

    private String exportReminders(DateRange range) {
        List<String> rows = new ArrayList<>();
        rows.add("id,userId,userEmail,title,scheduleTime,status,createdAt,updatedAt");
        reminderRepository.findCreatedBetween(range.fromDateTime(), range.toExclusive()).stream()
                .map(this::toReminderRow)
                .forEach(reminder -> rows.add(csv(
                        reminder.id(),
                        reminder.userId(),
                        reminder.userEmail(),
                        reminder.title(),
                        reminder.scheduleTime(),
                        reminder.status(),
                        reminder.createdAt(),
                        reminder.updatedAt()
                )));
        return String.join("\n", rows);
    }

    private String csv(Object... values) {
        return java.util.Arrays.stream(values)
                .map(value -> value == null ? "" : value.toString())
                .map(value -> "\"" + value.replace("\"", "\"\"") + "\"")
                .collect(Collectors.joining(","));
    }

    private record DateRange(
            LocalDate fromDate,
            LocalDate toDateInclusive,
            LocalDateTime fromDateTime,
            LocalDateTime toExclusive
    ) {
    }
}
