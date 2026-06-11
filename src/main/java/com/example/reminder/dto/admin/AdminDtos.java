package com.example.reminder.dto.admin;

import com.example.reminder.domain.enums.ReminderStatus;
import com.example.reminder.domain.enums.ReminderInstanceStatus;
import com.example.reminder.domain.enums.SafetyEventStatus;
import com.example.reminder.domain.enums.SafetyMethod;
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

    public record CheckInRowDto(
            Long id,
            Long userId,
            String userEmail,
            Long reminderId,
            String reminderTitle,
            Long scheduleId,
            LocalDateTime scheduledTime,
            LocalDateTime responseDeadline,
            LocalDateTime lastNotificationAt,
            LocalDateTime resolvedAt,
            ReminderInstanceStatus status,
            Integer escalationLevel,
            Integer missedCount,
            String responseAction,
            LocalDateTime responseTime
    ) {
    }

    public record CheckInSummaryDto(
            long scheduled,
            long completed,
            long missed,
            long escalated,
            BigDecimal successRate,
            long checkInsToday
    ) {
    }

    public record AdminStatusUpdateRequest(@NotNull String status) {
    }

    public record SafetyAlertRowDto(
            Long id,
            Long userId,
            String userEmail,
            Long reminderId,
            String reminderTitle,
            Long instanceId,
            Long trustedContactId,
            String trustedContactName,
            SafetyMethod method,
            SafetyEventStatus status,
            LocalDateTime triggeredAt,
            String locationUrl
    ) {
    }

    public record SafetyAlertSummaryDto(
            long openAlerts,
            long sentToday,
            long failedDelivery,
            long resolvedAlerts,
            BigDecimal avgResponseMinutes
    ) {
    }

    public record TrustedContactAdminDto(
            Long id,
            Long userId,
            String fullName,
            String relationship,
            String phone,
            String email,
            Integer priority,
            Boolean isActive,
            LocalDateTime createdAt
    ) {
    }

    public record AdminPlanRowDto(
            Long id,
            String name,
            BigDecimal price,
            String billingCycle,
            Integer maxReminders,
            Integer maxTrustedContacts,
            Integer maxDigitalAssets,
            String features,
            Boolean active,
            LocalDateTime createdAt,
            LocalDateTime archivedAt
    ) {
    }

    public record AdminPlanSummaryDto(
            long activePlans,
            long paidUsers,
            String topPlan,
            long draftChanges
    ) {
    }

    public record AdminPlanRequest(
            @NotNull String name,
            @NotNull BigDecimal price,
            @NotNull String billingCycle,
            @NotNull Integer maxReminders,
            @NotNull Integer maxTrustedContacts,
            @NotNull Integer maxDigitalAssets,
            String features,
            Boolean active
    ) {
    }

    public record AdminPlanPriceRequest(
            @NotNull BigDecimal price,
            String billingCycle
    ) {
    }

    public record NotificationTemplateDto(
            Long id,
            String eventType,
            String channel,
            String locale,
            String subject,
            String body,
            List<String> variables,
            Boolean active,
            LocalDateTime updatedAt
    ) {
    }

    public record NotificationTemplateRequest(
            String eventType,
            String channel,
            String locale,
            String subject,
            String body,
            List<String> variables,
            Boolean active
    ) {
    }

    public record NotificationPreviewRequest(Map<String, Object> variables) {
    }

    public record NotificationPreviewDto(
            String subject,
            String body
    ) {
    }

    public record NotificationLogDto(
            Long id,
            Long userId,
            String eventType,
            String channel,
            String status,
            String recipient,
            String providerResponse,
            LocalDateTime createdAt
    ) {
    }

    public record NotificationSummaryDto(
            long sentToday,
            long failed,
            long templates,
            long providers
    ) {
    }

    public record AuditLogRowDto(
            Long id,
            String actor,
            String action,
            String targetType,
            String targetId,
            String status,
            String metadata,
            LocalDateTime createdAt
    ) {
    }

    public record AuditLogSummaryDto(
            long eventsToday,
            long failedActions,
            long sensitiveChanges,
            long exports
    ) {
    }

    public record AuditLogCreateRequest(
            String actor,
            String action,
            String targetType,
            String targetId,
            String status,
            String metadata
    ) {
    }
}
