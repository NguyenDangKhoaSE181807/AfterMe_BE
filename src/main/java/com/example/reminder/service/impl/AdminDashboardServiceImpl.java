package com.example.reminder.service.impl;

import com.example.reminder.domain.enums.ReminderInstanceStatus;
import com.example.reminder.domain.enums.ReminderStatus;
import com.example.reminder.domain.enums.UserRole;
import com.example.reminder.domain.enums.UserStatus;
import com.example.reminder.dto.admin.AdminDtos;
import com.example.reminder.dto.common.PagedResponseDto;
import com.example.reminder.entity.AssetAccessForensicLog;
import com.example.reminder.entity.AssetAccessLog;
import com.example.reminder.entity.Plan;
import com.example.reminder.entity.Reminder;
import com.example.reminder.entity.ReminderInstance;
import com.example.reminder.entity.Transaction;
import com.example.reminder.entity.User;
import com.example.reminder.entity.UserSubscription;
import com.example.reminder.exception.BadRequestException;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.repository.AssetAccessForensicLogRepository;
import com.example.reminder.repository.AssetAccessLogRepository;
import com.example.reminder.repository.PlanRepository;
import com.example.reminder.repository.ReminderInstanceRepository;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.TransactionRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.repository.UserSubscriptionRepository;
import com.example.reminder.service.AdminDashboardService;
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
        LocalDateTime scheduleTime = reminderInstanceRepository.findByReminderIdAndDeletedAtIsNull(reminder.getId())
                .stream()
                .filter(instance -> instance.getScheduledTime() != null)
                .map(ReminderInstance::getScheduledTime)
                .filter(time -> !time.isBefore(LocalDateTime.now()))
                .min(LocalDateTime::compareTo)
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
