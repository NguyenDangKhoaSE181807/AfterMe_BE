package com.example.reminder.scheduler;

import com.example.reminder.service.SafetyEscalationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SafetyEscalationScheduler {

    private final SafetyEscalationService safetyEscalationService;

    @Scheduled(fixedDelayString = "${app.safety.escalation-scan-delay-ms:60000}")
    public void processDueEscalations() {
        try {
            safetyEscalationService.processDueEscalations();
        } catch (Exception ex) {
            log.error("Failed to process safety escalations", ex);
        }
    }
}
