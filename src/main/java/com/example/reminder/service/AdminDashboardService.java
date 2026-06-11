package com.example.reminder.service;

import com.example.reminder.domain.enums.ReminderStatus;
import com.example.reminder.domain.enums.SafetyEventStatus;
import com.example.reminder.domain.enums.UserRole;
import com.example.reminder.domain.enums.UserStatus;
import com.example.reminder.dto.admin.AdminDtos;
import com.example.reminder.dto.common.PagedResponseDto;
import java.time.LocalDate;

public interface AdminDashboardService {

    AdminDtos.DashboardOverviewDto getOverview(LocalDate from, LocalDate to, String interval);

    AdminDtos.DashboardCardsDto getOverviewCards();

    java.util.List<AdminDtos.TimeSeriesPointDto> getUserTimeseries(LocalDate from, LocalDate to, String interval);

    java.util.List<AdminDtos.TimeSeriesPointDto> getSubscriptionTimeseries(LocalDate from, LocalDate to, String interval);

    java.util.List<AdminDtos.TimeSeriesPointDto> getRevenueTimeseries(LocalDate from, LocalDate to, String interval);

    java.util.List<AdminDtos.TimeSeriesPointDto> getChurnTimeseries(LocalDate from, LocalDate to, String interval);

    PagedResponseDto<AdminDtos.AdminUserRowDto> getUsers(String q, UserStatus status, UserRole role, int page, int size);

    AdminDtos.AdminUserDetailDto getUser(Long id);

    AdminDtos.AdminUserSummaryDto getUserSummary();

    AdminDtos.AdminUserRowDto updateUser(Long id, AdminDtos.AdminUserUpdateRequest request);

    AdminDtos.AdminUserRowDto updateUserStatus(Long id, UserStatus status);

    AdminDtos.AdminUserRowDto resetUserSubscription(Long id);

    PagedResponseDto<AdminDtos.SubscriptionRowDto> getSubscriptions(String q, String status, Long planId, int page, int size);

    AdminDtos.SubscriptionRowDto getSubscription(Long id);

    AdminDtos.SubscriptionSummaryDto getSubscriptionSummary();

    AdminDtos.SubscriptionAnalyticsDto getSubscriptionAnalytics(LocalDate from, LocalDate to, String interval);

    AdminDtos.SubscriptionRowDto changeSubscriptionPlan(Long id, AdminDtos.SubscriptionPlanChangeRequest request);

    AdminDtos.SubscriptionRowDto extendSubscription(Long id, int days);

    AdminDtos.SubscriptionRowDto cancelSubscription(Long id);

    AdminDtos.SubscriptionRowDto reactivateSubscription(Long id);

    PagedResponseDto<AdminDtos.ReminderRowDto> getReminders(String q, ReminderStatus status, Long userId, int page, int size);

    AdminDtos.ReminderRowDto getReminder(Long id);

    AdminDtos.ReminderSummaryDto getReminderSummary();

    java.util.List<AdminDtos.TimeSeriesPointDto> getReminderTimeseries(LocalDate from, LocalDate to, String interval);

    AdminDtos.ReminderExecutionStatsDto getReminderExecutionStats(LocalDate from, LocalDate to);

    AdminDtos.ReminderRowDto updateReminderStatus(Long id, ReminderStatus status);

    AdminDtos.FinanceSummaryDto getFinanceSummary();

    java.util.List<AdminDtos.RevenueByPlanDto> getRevenueByPlan(LocalDate from, LocalDate to);

    PagedResponseDto<AdminDtos.TransactionRowDto> getTransactions(String q, String status, LocalDate from, LocalDate to, int page, int size);

    AdminDtos.TransactionRowDto getTransaction(Long id);

    AdminDtos.TransactionSummaryDto getTransactionSummary(LocalDate from, LocalDate to);

    AdminDtos.AdminSettingsDto getSettings();

    AdminDtos.AdminSettingsDto updateSettings(AdminDtos.AdminSettingsDto request);

    AdminDtos.ReportOverviewDto getReportsOverview();

    String exportReport(String type, LocalDate from, LocalDate to);

    PagedResponseDto<AdminDtos.ActivityLogDto> getActivityLog(int page, int size);

    PagedResponseDto<AdminDtos.CheckInRowDto> getCheckIns(String status, Long userId, LocalDate from, LocalDate to, int page, int size);

    AdminDtos.CheckInSummaryDto getCheckInSummary(LocalDate from, LocalDate to);

    java.util.List<AdminDtos.TimeSeriesPointDto> getCheckInTimeseries(LocalDate from, LocalDate to, String interval);

    PagedResponseDto<AdminDtos.CheckInRowDto> getReminderExecutions(Long reminderId, int page, int size);

    AdminDtos.CheckInRowDto retryCheckIn(Long id);

    AdminDtos.CheckInRowDto updateCheckInStatus(Long id, String status);

    PagedResponseDto<AdminDtos.SafetyAlertRowDto> getSafetyAlerts(SafetyEventStatus status, Long userId, LocalDate from, LocalDate to, int page, int size);

    AdminDtos.SafetyAlertSummaryDto getSafetyAlertSummary(LocalDate from, LocalDate to);

    AdminDtos.SafetyAlertRowDto getSafetyAlert(Long id);

    AdminDtos.SafetyAlertRowDto updateSafetyAlertStatus(Long id, String status);

    AdminDtos.SafetyAlertRowDto resendSafetyAlert(Long id);

    java.util.List<AdminDtos.TrustedContactAdminDto> getUserTrustedContacts(Long userId);

    java.util.List<AdminDtos.AdminPlanRowDto> getAdminPlans(Boolean active, String billingCycle);

    AdminDtos.AdminPlanSummaryDto getAdminPlanSummary();

    AdminDtos.AdminPlanRowDto createAdminPlan(AdminDtos.AdminPlanRequest request);

    AdminDtos.AdminPlanRowDto getAdminPlan(Long id);

    AdminDtos.AdminPlanRowDto updateAdminPlan(Long id, AdminDtos.AdminPlanRequest request);

    AdminDtos.AdminPlanRowDto createAdminPlanPrice(Long id, AdminDtos.AdminPlanPriceRequest request);

    AdminDtos.AdminPlanRowDto archiveAdminPlan(Long id);

    java.util.List<AdminDtos.NotificationTemplateDto> getNotificationTemplates(String eventType, String channel, String locale);

    AdminDtos.NotificationTemplateDto createNotificationTemplate(AdminDtos.NotificationTemplateRequest request);

    AdminDtos.NotificationTemplateDto updateNotificationTemplate(Long id, AdminDtos.NotificationTemplateRequest request);

    AdminDtos.NotificationPreviewDto previewNotificationTemplate(Long id, AdminDtos.NotificationPreviewRequest request);

    AdminDtos.NotificationSummaryDto getNotificationSummary();

    PagedResponseDto<AdminDtos.NotificationLogDto> getNotificationLogs(String channel, String status, Long userId, String eventType, LocalDate from, LocalDate to, int page, int size);

    AdminDtos.NotificationLogDto retryNotificationLog(Long id);

    PagedResponseDto<AdminDtos.AuditLogRowDto> getAuditLogs(String actor, String action, String targetType, String status, LocalDate from, LocalDate to, int page, int size);

    AdminDtos.AuditLogSummaryDto getAuditLogSummary(LocalDate from, LocalDate to);

    AdminDtos.AuditLogRowDto getAuditLog(Long id);

    String exportAuditLogs(String actor, String action, String targetType, String status, LocalDate from, LocalDate to);

    AdminDtos.AuditLogRowDto createAuditLog(AdminDtos.AuditLogCreateRequest request);
}
