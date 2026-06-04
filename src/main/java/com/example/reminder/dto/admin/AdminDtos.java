package com.example.reminder.dto.admin;

import com.example.reminder.domain.enums.ReminderStatus;
import com.example.reminder.domain.enums.TonePreference;
import com.example.reminder.domain.enums.UserRole;
import com.example.reminder.domain.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record DashboardOverviewDto(
            DashboardCardsDto cards,
            List<TimeSeriesPointDto> userGrowth,
            List<TimeSeriesPointDto> subscriptionGrowth,
            List<TimeSeriesPointDto> revenueTrend,
            List<TimeSeriesPointDto> churnTrend
    ) {
    }

    public record DashboardCardsDto(
            long totalUsers,
            long activeUsers,
            long premiumUsers,
            long activeSubscriptions,
            BigDecimal mrr,
            BigDecimal revenueToday,
            BigDecimal revenueThisMonth,
            long failedPayments,
            long remindersSentToday
    ) {
    }

    public record TimeSeriesPointDto(
            String period,
            long count,
            BigDecimal amount,
            BigDecimal rate
    ) {
    }

    public record AdminUserRowDto(
            Long id,
            String email,
            String fullName,
            TonePreference tonePreference,
            UserStatus status,
            UserRole role,
            String currentPlanName,
            LocalDateTime planExpiresAt,
            LocalDateTime createdAt
    ) {
    }

    public record AdminUserDetailDto(
            AdminUserRowDto user,
            long subscriptionCount,
            long reminderCount,
            BigDecimal totalRevenue
    ) {
    }

    public record AdminUserSummaryDto(
            long totalUsers,
            long activeUsers,
            long pendingUsers,
            long suspendedUsers,
            long premiumUsers
    ) {
    }

    public record AdminUserUpdateRequest(
            String email,
            String fullName,
            TonePreference tonePreference,
            UserStatus status,
            UserRole role
    ) {
    }

    public record AdminUserStatusRequest(@NotNull UserStatus status) {
    }

    public record SubscriptionRowDto(
            Long id,
            Long userId,
            String userEmail,
            String userFullName,
            Long planId,
            String planName,
            String billingCycle,
            BigDecimal planPrice,
            LocalDateTime startedAt,
            LocalDateTime expiresAt,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record SubscriptionSummaryDto(
            long total,
            long active,
            long trial,
            long expired,
            long cancelled,
            long pending,
            long failed
    ) {
    }

    public record SubscriptionAnalyticsDto(
            List<TimeSeriesPointDto> subscriptionGrowth,
            BigDecimal conversionRate,
            BigDecimal churnRate
    ) {
    }

    public record SubscriptionPlanChangeRequest(Long planId, String planName) {
    }

    public record SubscriptionExtendRequest(@Positive int days) {
    }

    public record ReminderRowDto(
            Long id,
            Long userId,
            String userEmail,
            String title,
            LocalDateTime scheduleTime,
            ReminderStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record ReminderSummaryDto(
            long totalReminders,
            long activeReminders,
            long sentToday,
            BigDecimal successRate
    ) {
    }

    public record ReminderExecutionStatsDto(
            long totalInstances,
            long scheduledToday,
            long sentToday,
            long successful,
            long missed,
            long escalated,
            BigDecimal successRate
    ) {
    }

    public record ReminderStatusRequest(@NotNull ReminderStatus status) {
    }

    public record FinanceSummaryDto(
            BigDecimal revenueToday,
            BigDecimal revenueMtd,
            BigDecimal revenueYtd,
            BigDecimal mrr,
            BigDecimal arr
    ) {
    }

    public record RevenueByPlanDto(
            Long planId,
            String planName,
            BigDecimal revenue,
            long transactions
    ) {
    }

    public record TransactionRowDto(
            Long id,
            Long userId,
            String userEmail,
            BigDecimal amount,
            String currency,
            String provider,
            String status,
            String transactionRef,
            LocalDateTime paidAt,
            LocalDateTime createdAt
    ) {
    }

    public record TransactionSummaryDto(
            long total,
            long success,
            long failed,
            long pending,
            long refunded,
            BigDecimal successfulRevenue
    ) {
    }

    public record AdminSettingsDto(Map<String, Object> values) {
    }

    public record ReportOverviewDto(
            DashboardCardsDto dashboard,
            AdminUserSummaryDto users,
            SubscriptionSummaryDto subscriptions,
            ReminderSummaryDto reminders,
            FinanceSummaryDto finance
    ) {
    }

    public record ActivityLogDto(
            Long id,
            String actor,
            String action,
            String target,
            LocalDateTime createdAt
    ) {
    }
}
