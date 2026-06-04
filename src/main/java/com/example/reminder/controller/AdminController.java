package com.example.reminder.controller;

import com.example.reminder.domain.enums.ReminderStatus;
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
