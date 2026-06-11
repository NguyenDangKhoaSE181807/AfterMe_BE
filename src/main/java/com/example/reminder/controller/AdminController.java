package com.example.reminder.controller;

import com.example.reminder.domain.enums.ReminderStatus;
import com.example.reminder.domain.enums.SafetyEventStatus;
import com.example.reminder.domain.enums.UserRole;
import com.example.reminder.domain.enums.UserStatus;
import com.example.reminder.dto.admin.AdminDtos;
import com.example.reminder.dto.common.BaseResponse;
import com.example.reminder.dto.common.PagedResponseDto;
import com.example.reminder.service.AdminDashboardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/dashboard/overview")
    public ResponseEntity<BaseResponse<AdminDtos.DashboardOverviewDto>> getOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") String interval,
            HttpServletRequest request
    ) {
        return ok("ADMIN_DASHBOARD_OVERVIEW_FOUND", "Admin dashboard overview retrieved",
                adminDashboardService.getOverview(from, to, interval), request);
    }

    @GetMapping("/dashboard/overview/cards")
    public ResponseEntity<BaseResponse<AdminDtos.DashboardCardsDto>> getOverviewCards(HttpServletRequest request) {
        return ok("ADMIN_DASHBOARD_CARDS_FOUND", "Admin dashboard cards retrieved",
                adminDashboardService.getOverviewCards(), request);
    }

    @GetMapping("/dashboard/users/timeseries")
    public ResponseEntity<BaseResponse<List<AdminDtos.TimeSeriesPointDto>>> getUserTimeseries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") String interval,
            HttpServletRequest request
    ) {
        return ok("ADMIN_USER_TIMESERIES_FOUND", "User timeseries retrieved",
                adminDashboardService.getUserTimeseries(from, to, interval), request);
    }

    @GetMapping("/dashboard/subscriptions/timeseries")
    public ResponseEntity<BaseResponse<List<AdminDtos.TimeSeriesPointDto>>> getSubscriptionTimeseries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") String interval,
            HttpServletRequest request
    ) {
        return ok("ADMIN_SUBSCRIPTION_TIMESERIES_FOUND", "Subscription timeseries retrieved",
                adminDashboardService.getSubscriptionTimeseries(from, to, interval), request);
    }

    @GetMapping("/dashboard/revenue/timeseries")
    public ResponseEntity<BaseResponse<List<AdminDtos.TimeSeriesPointDto>>> getRevenueTimeseries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") String interval,
            HttpServletRequest request
    ) {
        return ok("ADMIN_REVENUE_TIMESERIES_FOUND", "Revenue timeseries retrieved",
                adminDashboardService.getRevenueTimeseries(from, to, interval), request);
    }

    @GetMapping("/dashboard/churn/timeseries")
    public ResponseEntity<BaseResponse<List<AdminDtos.TimeSeriesPointDto>>> getChurnTimeseries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") String interval,
            HttpServletRequest request
    ) {
        return ok("ADMIN_CHURN_TIMESERIES_FOUND", "Churn timeseries retrieved",
                adminDashboardService.getChurnTimeseries(from, to, interval), request);
    }

    @GetMapping("/users")
    public ResponseEntity<BaseResponse<PagedResponseDto<AdminDtos.AdminUserRowDto>>> getUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) UserRole role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        return ok("ADMIN_USERS_FOUND", "Admin users retrieved",
                adminDashboardService.getUsers(q, status, role, page, size), request);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<BaseResponse<AdminDtos.AdminUserDetailDto>> getUser(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        return ok("ADMIN_USER_FOUND", "Admin user retrieved", adminDashboardService.getUser(id), request);
    }

    @GetMapping("/users/summary")
    public ResponseEntity<BaseResponse<AdminDtos.AdminUserSummaryDto>> getUserSummary(HttpServletRequest request) {
        return ok("ADMIN_USERS_SUMMARY_FOUND", "Admin users summary retrieved",
                adminDashboardService.getUserSummary(), request);
    }

    @PatchMapping("/users/{id}")
    public ResponseEntity<BaseResponse<AdminDtos.AdminUserRowDto>> updateUser(
            @PathVariable Long id,
            @RequestBody AdminDtos.AdminUserUpdateRequest body,
            HttpServletRequest request
    ) {
        return ok("ADMIN_USER_UPDATED", "Admin user updated",
                adminDashboardService.updateUser(id, body), request);
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<BaseResponse<AdminDtos.AdminUserRowDto>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminDtos.AdminUserStatusRequest body,
            HttpServletRequest request
    ) {
        return ok("ADMIN_USER_STATUS_UPDATED", "Admin user status updated",
                adminDashboardService.updateUserStatus(id, body.status()), request);
    }

    @PostMapping("/users/{id}/subscription/reset")
    public ResponseEntity<BaseResponse<AdminDtos.AdminUserRowDto>> resetUserSubscription(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        return ok("ADMIN_USER_SUBSCRIPTION_RESET", "Admin user subscription reset",
                adminDashboardService.resetUserSubscription(id), request);
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<BaseResponse<PagedResponseDto<AdminDtos.SubscriptionRowDto>>> getSubscriptions(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long planId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        return ok("ADMIN_SUBSCRIPTIONS_FOUND", "Admin subscriptions retrieved",
                adminDashboardService.getSubscriptions(q, status, planId, page, size), request);
    }

    @GetMapping("/subscriptions/{id}")
    public ResponseEntity<BaseResponse<AdminDtos.SubscriptionRowDto>> getSubscription(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        return ok("ADMIN_SUBSCRIPTION_FOUND", "Admin subscription retrieved",
                adminDashboardService.getSubscription(id), request);
    }

    @GetMapping("/subscriptions/summary")
    public ResponseEntity<BaseResponse<AdminDtos.SubscriptionSummaryDto>> getSubscriptionSummary(HttpServletRequest request) {
        return ok("ADMIN_SUBSCRIPTIONS_SUMMARY_FOUND", "Admin subscriptions summary retrieved",
                adminDashboardService.getSubscriptionSummary(), request);
    }

    @GetMapping("/subscriptions/analytics")
    public ResponseEntity<BaseResponse<AdminDtos.SubscriptionAnalyticsDto>> getSubscriptionAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") String interval,
            HttpServletRequest request
    ) {
        return ok("ADMIN_SUBSCRIPTIONS_ANALYTICS_FOUND", "Admin subscriptions analytics retrieved",
                adminDashboardService.getSubscriptionAnalytics(from, to, interval), request);
    }

    @PostMapping({"/subscriptions/{id}/upgrade", "/subscriptions/{id}/downgrade"})
    public ResponseEntity<BaseResponse<AdminDtos.SubscriptionRowDto>> changeSubscriptionPlan(
            @PathVariable Long id,
            @Valid @RequestBody AdminDtos.SubscriptionPlanChangeRequest body,
            HttpServletRequest request
    ) {
        return ok("ADMIN_SUBSCRIPTION_PLAN_CHANGED", "Admin subscription plan changed",
                adminDashboardService.changeSubscriptionPlan(id, body), request);
    }

    @PostMapping("/subscriptions/{id}/extend")
    public ResponseEntity<BaseResponse<AdminDtos.SubscriptionRowDto>> extendSubscription(
            @PathVariable Long id,
            @Valid @RequestBody AdminDtos.SubscriptionExtendRequest body,
            HttpServletRequest request
    ) {
        return ok("ADMIN_SUBSCRIPTION_EXTENDED", "Admin subscription extended",
                adminDashboardService.extendSubscription(id, body.days()), request);
    }

    @PostMapping("/subscriptions/{id}/cancel")
    public ResponseEntity<BaseResponse<AdminDtos.SubscriptionRowDto>> cancelSubscription(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        return ok("ADMIN_SUBSCRIPTION_CANCELLED", "Admin subscription cancelled",
                adminDashboardService.cancelSubscription(id), request);
    }

    @PostMapping("/subscriptions/{id}/reactivate")
    public ResponseEntity<BaseResponse<AdminDtos.SubscriptionRowDto>> reactivateSubscription(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        return ok("ADMIN_SUBSCRIPTION_REACTIVATED", "Admin subscription reactivated",
                adminDashboardService.reactivateSubscription(id), request);
    }

    @GetMapping("/reminders")
    public ResponseEntity<BaseResponse<PagedResponseDto<AdminDtos.ReminderRowDto>>> getReminders(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ReminderStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        return ok("ADMIN_REMINDERS_FOUND", "Admin reminders retrieved",
                adminDashboardService.getReminders(q, status, userId, page, size), request);
    }

    @GetMapping("/reminders/{id}")
    public ResponseEntity<BaseResponse<AdminDtos.ReminderRowDto>> getReminder(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        return ok("ADMIN_REMINDER_FOUND", "Admin reminder retrieved",
                adminDashboardService.getReminder(id), request);
    }

    @GetMapping("/reminders/summary")
    public ResponseEntity<BaseResponse<AdminDtos.ReminderSummaryDto>> getReminderSummary(HttpServletRequest request) {
        return ok("ADMIN_REMINDERS_SUMMARY_FOUND", "Admin reminders summary retrieved",
                adminDashboardService.getReminderSummary(), request);
    }

    @GetMapping("/reminders/timeseries")
    public ResponseEntity<BaseResponse<List<AdminDtos.TimeSeriesPointDto>>> getReminderTimeseries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") String interval,
            HttpServletRequest request
    ) {
        return ok("ADMIN_REMINDER_TIMESERIES_FOUND", "Admin reminder timeseries retrieved",
                adminDashboardService.getReminderTimeseries(from, to, interval), request);
    }

    @GetMapping("/reminders/execution-stats")
    public ResponseEntity<BaseResponse<AdminDtos.ReminderExecutionStatsDto>> getReminderExecutionStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request
    ) {
        return ok("ADMIN_REMINDER_EXECUTION_STATS_FOUND", "Admin reminder execution stats retrieved",
                adminDashboardService.getReminderExecutionStats(from, to), request);
    }

    @PatchMapping("/reminders/{id}/status")
    public ResponseEntity<BaseResponse<AdminDtos.ReminderRowDto>> updateReminderStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminDtos.ReminderStatusRequest body,
            HttpServletRequest request
    ) {
        return ok("ADMIN_REMINDER_STATUS_UPDATED", "Admin reminder status updated",
                adminDashboardService.updateReminderStatus(id, body.status()), request);
    }

    @GetMapping("/finance/summary")
    public ResponseEntity<BaseResponse<AdminDtos.FinanceSummaryDto>> getFinanceSummary(HttpServletRequest request) {
        return ok("ADMIN_FINANCE_SUMMARY_FOUND", "Admin finance summary retrieved",
                adminDashboardService.getFinanceSummary(), request);
    }

    @GetMapping("/finance/revenue/timeseries")
    public ResponseEntity<BaseResponse<List<AdminDtos.TimeSeriesPointDto>>> getFinanceRevenueTimeseries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") String interval,
            HttpServletRequest request
    ) {
        return ok("ADMIN_FINANCE_REVENUE_TIMESERIES_FOUND", "Admin finance revenue timeseries retrieved",
                adminDashboardService.getRevenueTimeseries(from, to, interval), request);
    }

    @GetMapping("/finance/revenue/by-plan")
    public ResponseEntity<BaseResponse<List<AdminDtos.RevenueByPlanDto>>> getRevenueByPlan(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request
    ) {
        return ok("ADMIN_REVENUE_BY_PLAN_FOUND", "Admin revenue by plan retrieved",
                adminDashboardService.getRevenueByPlan(from, to), request);
    }

    @GetMapping("/finance/transactions")
    public ResponseEntity<BaseResponse<PagedResponseDto<AdminDtos.TransactionRowDto>>> getTransactions(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        return ok("ADMIN_TRANSACTIONS_FOUND", "Admin transactions retrieved",
                adminDashboardService.getTransactions(q, status, from, to, page, size), request);
    }

    @GetMapping("/finance/transactions/{id}")
    public ResponseEntity<BaseResponse<AdminDtos.TransactionRowDto>> getTransaction(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        return ok("ADMIN_TRANSACTION_FOUND", "Admin transaction retrieved",
                adminDashboardService.getTransaction(id), request);
    }

    @GetMapping("/finance/transactions/summary")
    public ResponseEntity<BaseResponse<AdminDtos.TransactionSummaryDto>> getTransactionSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request
    ) {
        return ok("ADMIN_TRANSACTIONS_SUMMARY_FOUND", "Admin transactions summary retrieved",
                adminDashboardService.getTransactionSummary(from, to), request);
    }

    @GetMapping("/settings")
    public ResponseEntity<BaseResponse<AdminDtos.AdminSettingsDto>> getSettings(HttpServletRequest request) {
        return ok("ADMIN_SETTINGS_FOUND", "Admin settings retrieved",
                adminDashboardService.getSettings(), request);
    }

    @PatchMapping("/settings")
    public ResponseEntity<BaseResponse<AdminDtos.AdminSettingsDto>> updateSettings(
            @RequestBody AdminDtos.AdminSettingsDto body,
            HttpServletRequest request
    ) {
        return ok("ADMIN_SETTINGS_UPDATED", "Admin settings updated",
                adminDashboardService.updateSettings(body), request);
    }

    @GetMapping("/reports/overview")
    public ResponseEntity<BaseResponse<AdminDtos.ReportOverviewDto>> getReportsOverview(HttpServletRequest request) {
        return ok("ADMIN_REPORTS_OVERVIEW_FOUND", "Admin reports overview retrieved",
                adminDashboardService.getReportsOverview(), request);
    }

    @GetMapping("/reports/export")
    public ResponseEntity<String> exportReport(
            @RequestParam String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String csv = adminDashboardService.exportReport(type, from, to);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"admin-" + type + ".csv\"")
                .body(csv);
    }

    @GetMapping("/reports/activity-log")
    public ResponseEntity<BaseResponse<PagedResponseDto<AdminDtos.ActivityLogDto>>> getActivityLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        return ok("ADMIN_ACTIVITY_LOG_FOUND", "Admin activity log retrieved",
                adminDashboardService.getActivityLog(page, size), request);
    }

    @GetMapping("/check-ins")
    public ResponseEntity<BaseResponse<PagedResponseDto<AdminDtos.CheckInRowDto>>> getCheckIns(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        return ok("ADMIN_CHECK_INS_FOUND", "Admin check-ins retrieved",
                adminDashboardService.getCheckIns(status, userId, from, to, page, size), request);
    }

    @GetMapping("/check-ins/summary")
    public ResponseEntity<BaseResponse<AdminDtos.CheckInSummaryDto>> getCheckInSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request
    ) {
        return ok("ADMIN_CHECK_INS_SUMMARY_FOUND", "Admin check-in summary retrieved",
                adminDashboardService.getCheckInSummary(from, to), request);
    }

    @GetMapping("/check-ins/timeseries")
    public ResponseEntity<BaseResponse<List<AdminDtos.TimeSeriesPointDto>>> getCheckInTimeseries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") String interval,
            HttpServletRequest request
    ) {
        return ok("ADMIN_CHECK_INS_TIMESERIES_FOUND", "Admin check-in timeseries retrieved",
                adminDashboardService.getCheckInTimeseries(from, to, interval), request);
    }

    @GetMapping("/reminders/{id}/executions")
    public ResponseEntity<BaseResponse<PagedResponseDto<AdminDtos.CheckInRowDto>>> getReminderExecutions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        return ok("ADMIN_REMINDER_EXECUTIONS_FOUND", "Admin reminder executions retrieved",
                adminDashboardService.getReminderExecutions(id, page, size), request);
    }

    @PostMapping("/check-ins/{id}/retry")
    public ResponseEntity<BaseResponse<AdminDtos.CheckInRowDto>> retryCheckIn(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        return ok("ADMIN_CHECK_IN_RETRIED", "Admin check-in retry queued",
                adminDashboardService.retryCheckIn(id), request);
    }

    @PatchMapping("/check-ins/{id}/status")
    public ResponseEntity<BaseResponse<AdminDtos.CheckInRowDto>> updateCheckInStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminDtos.AdminStatusUpdateRequest body,
            HttpServletRequest request
    ) {
        return ok("ADMIN_CHECK_IN_STATUS_UPDATED", "Admin check-in status updated",
                adminDashboardService.updateCheckInStatus(id, body.status()), request);
    }

    @GetMapping("/safety/alerts")
    public ResponseEntity<BaseResponse<PagedResponseDto<AdminDtos.SafetyAlertRowDto>>> getSafetyAlerts(
            @RequestParam(required = false) SafetyEventStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        return ok("ADMIN_SAFETY_ALERTS_FOUND", "Admin safety alerts retrieved",
                adminDashboardService.getSafetyAlerts(status, userId, from, to, page, size), request);
    }

    @GetMapping("/safety/alerts/summary")
    public ResponseEntity<BaseResponse<AdminDtos.SafetyAlertSummaryDto>> getSafetyAlertSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request
    ) {
        return ok("ADMIN_SAFETY_ALERTS_SUMMARY_FOUND", "Admin safety alerts summary retrieved",
                adminDashboardService.getSafetyAlertSummary(from, to), request);
    }

    @GetMapping("/safety/alerts/{id}")
    public ResponseEntity<BaseResponse<AdminDtos.SafetyAlertRowDto>> getSafetyAlert(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        return ok("ADMIN_SAFETY_ALERT_FOUND", "Admin safety alert retrieved",
                adminDashboardService.getSafetyAlert(id), request);
    }

    @PatchMapping("/safety/alerts/{id}/status")
    public ResponseEntity<BaseResponse<AdminDtos.SafetyAlertRowDto>> updateSafetyAlertStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminDtos.AdminStatusUpdateRequest body,
            HttpServletRequest request
    ) {
        return ok("ADMIN_SAFETY_ALERT_STATUS_UPDATED", "Admin safety alert status updated",
                adminDashboardService.updateSafetyAlertStatus(id, body.status()), request);
    }

    @PostMapping("/safety/alerts/{id}/resend")
    public ResponseEntity<BaseResponse<AdminDtos.SafetyAlertRowDto>> resendSafetyAlert(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        return ok("ADMIN_SAFETY_ALERT_RESENT", "Admin safety alert resend queued",
                adminDashboardService.resendSafetyAlert(id), request);
    }

    @GetMapping("/users/{id}/trusted-contacts")
    public ResponseEntity<BaseResponse<List<AdminDtos.TrustedContactAdminDto>>> getUserTrustedContacts(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        return ok("ADMIN_USER_TRUSTED_CONTACTS_FOUND", "Admin user trusted contacts retrieved",
                adminDashboardService.getUserTrustedContacts(id), request);
    }

    @GetMapping("/plans")
    public ResponseEntity<BaseResponse<List<AdminDtos.AdminPlanRowDto>>> getAdminPlans(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String billingCycle,
            HttpServletRequest request
    ) {
        return ok("ADMIN_PLANS_FOUND", "Admin plans retrieved",
                adminDashboardService.getAdminPlans(active, billingCycle), request);
    }

    @GetMapping("/plans/summary")
    public ResponseEntity<BaseResponse<AdminDtos.AdminPlanSummaryDto>> getAdminPlanSummary(HttpServletRequest request) {
        return ok("ADMIN_PLANS_SUMMARY_FOUND", "Admin plans summary retrieved",
                adminDashboardService.getAdminPlanSummary(), request);
    }

    @PostMapping("/plans")
    public ResponseEntity<BaseResponse<AdminDtos.AdminPlanRowDto>> createAdminPlan(
            @Valid @RequestBody AdminDtos.AdminPlanRequest body,
            HttpServletRequest request
    ) {
        return ok("ADMIN_PLAN_CREATED", "Admin plan created",
                adminDashboardService.createAdminPlan(body), request);
    }

    @GetMapping("/plans/{id}")
    public ResponseEntity<BaseResponse<AdminDtos.AdminPlanRowDto>> getAdminPlan(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        return ok("ADMIN_PLAN_FOUND", "Admin plan retrieved",
                adminDashboardService.getAdminPlan(id), request);
    }

    @PatchMapping("/plans/{id}")
    public ResponseEntity<BaseResponse<AdminDtos.AdminPlanRowDto>> updateAdminPlan(
            @PathVariable Long id,
            @Valid @RequestBody AdminDtos.AdminPlanRequest body,
            HttpServletRequest request
    ) {
        return ok("ADMIN_PLAN_UPDATED", "Admin plan updated",
                adminDashboardService.updateAdminPlan(id, body), request);
    }

    @PostMapping("/plans/{id}/prices")
    public ResponseEntity<BaseResponse<AdminDtos.AdminPlanRowDto>> createAdminPlanPrice(
            @PathVariable Long id,
            @Valid @RequestBody AdminDtos.AdminPlanPriceRequest body,
            HttpServletRequest request
    ) {
        return ok("ADMIN_PLAN_PRICE_CREATED", "Admin plan price created",
                adminDashboardService.createAdminPlanPrice(id, body), request);
    }

    @PostMapping("/plans/{id}/archive")
    public ResponseEntity<BaseResponse<AdminDtos.AdminPlanRowDto>> archiveAdminPlan(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        return ok("ADMIN_PLAN_ARCHIVED", "Admin plan archived",
                adminDashboardService.archiveAdminPlan(id), request);
    }

    @GetMapping("/notifications/summary")
    public ResponseEntity<BaseResponse<AdminDtos.NotificationSummaryDto>> getNotificationSummary(HttpServletRequest request) {
        return ok("ADMIN_NOTIFICATIONS_SUMMARY_FOUND", "Admin notification summary retrieved",
                adminDashboardService.getNotificationSummary(), request);
    }

    @GetMapping("/notifications/templates")
    public ResponseEntity<BaseResponse<List<AdminDtos.NotificationTemplateDto>>> getNotificationTemplates(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String locale,
            HttpServletRequest request
    ) {
        return ok("ADMIN_NOTIFICATION_TEMPLATES_FOUND", "Admin notification templates retrieved",
                adminDashboardService.getNotificationTemplates(eventType, channel, locale), request);
    }

    @PostMapping("/notifications/templates")
    public ResponseEntity<BaseResponse<AdminDtos.NotificationTemplateDto>> createNotificationTemplate(
            @RequestBody AdminDtos.NotificationTemplateRequest body,
            HttpServletRequest request
    ) {
        return ok("ADMIN_NOTIFICATION_TEMPLATE_CREATED", "Admin notification template created",
                adminDashboardService.createNotificationTemplate(body), request);
    }

    @PatchMapping("/notifications/templates/{id}")
    public ResponseEntity<BaseResponse<AdminDtos.NotificationTemplateDto>> updateNotificationTemplate(
            @PathVariable Long id,
            @RequestBody AdminDtos.NotificationTemplateRequest body,
            HttpServletRequest request
    ) {
        return ok("ADMIN_NOTIFICATION_TEMPLATE_UPDATED", "Admin notification template updated",
                adminDashboardService.updateNotificationTemplate(id, body), request);
    }

    @PostMapping("/notifications/templates/{id}/preview")
    public ResponseEntity<BaseResponse<AdminDtos.NotificationPreviewDto>> previewNotificationTemplate(
            @PathVariable Long id,
            @RequestBody(required = false) AdminDtos.NotificationPreviewRequest body,
            HttpServletRequest request
    ) {
        return ok("ADMIN_NOTIFICATION_TEMPLATE_PREVIEWED", "Admin notification template preview generated",
                adminDashboardService.previewNotificationTemplate(id, body), request);
    }

    @GetMapping("/notifications/logs")
    public ResponseEntity<BaseResponse<PagedResponseDto<AdminDtos.NotificationLogDto>>> getNotificationLogs(
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        return ok("ADMIN_NOTIFICATION_LOGS_FOUND", "Admin notification logs retrieved",
                adminDashboardService.getNotificationLogs(channel, status, userId, eventType, from, to, page, size), request);
    }

    @PostMapping("/notifications/logs/{id}/retry")
    public ResponseEntity<BaseResponse<AdminDtos.NotificationLogDto>> retryNotificationLog(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        return ok("ADMIN_NOTIFICATION_LOG_RETRIED", "Admin notification retry queued",
                adminDashboardService.retryNotificationLog(id), request);
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<BaseResponse<PagedResponseDto<AdminDtos.AuditLogRowDto>>> getAuditLogs(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        return ok("ADMIN_AUDIT_LOGS_FOUND", "Admin audit logs retrieved",
                adminDashboardService.getAuditLogs(actor, action, targetType, status, from, to, page, size), request);
    }

    @GetMapping("/audit-logs/summary")
    public ResponseEntity<BaseResponse<AdminDtos.AuditLogSummaryDto>> getAuditLogSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request
    ) {
        return ok("ADMIN_AUDIT_LOGS_SUMMARY_FOUND", "Admin audit log summary retrieved",
                adminDashboardService.getAuditLogSummary(from, to), request);
    }

    @GetMapping("/audit-logs/{id}")
    public ResponseEntity<BaseResponse<AdminDtos.AuditLogRowDto>> getAuditLog(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        return ok("ADMIN_AUDIT_LOG_FOUND", "Admin audit log retrieved",
                adminDashboardService.getAuditLog(id), request);
    }

    @GetMapping("/audit-logs/export")
    public ResponseEntity<String> exportAuditLogs(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"admin-audit-logs.csv\"")
                .body(adminDashboardService.exportAuditLogs(actor, action, targetType, status, from, to));
    }

    @PostMapping("/audit-logs")
    public ResponseEntity<BaseResponse<AdminDtos.AuditLogRowDto>> createAuditLog(
            @RequestBody AdminDtos.AuditLogCreateRequest body,
            HttpServletRequest request
    ) {
        return ok("ADMIN_AUDIT_LOG_CREATED", "Admin audit log created",
                adminDashboardService.createAuditLog(body), request);
    }

    private <T> ResponseEntity<BaseResponse<T>> ok(String code, String message, T data, HttpServletRequest request) {
        return ResponseEntity.ok(BaseResponse.<T>builder()
                .success(true)
                .code(code)
                .message(message)
                .data(data)
                .errors(null)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .requestId(request.getHeader("X-Request-Id"))
                .build());
    }
}
