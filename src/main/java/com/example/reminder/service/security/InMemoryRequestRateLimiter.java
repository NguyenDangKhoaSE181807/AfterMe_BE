package com.example.reminder.service.security;

import com.example.reminder.exception.TooManyRequestsException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class InMemoryRequestRateLimiter implements RequestRateLimiter {

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final int windowSeconds;
    private final int maxRequestsPerWindow;

    public InMemoryRequestRateLimiter(
            @Value("${app.security.rate-limit.window-seconds:60}") int windowSeconds,
            @Value("${app.security.rate-limit.max-requests-per-window:20}") int maxRequestsPerWindow
    ) {
        this.windowSeconds = Math.max(1, windowSeconds);
        this.maxRequestsPerWindow = Math.max(1, maxRequestsPerWindow);
    }

    @Override
    public void checkRateLimit(String action, String actorId, String ipAddress) {
        String safeActorId = actorId == null ? "unknown-actor" : actorId;
        String safeIp = ipAddress == null ? "unknown-ip" : ipAddress;
        String key = action + "|" + safeActorId + "|" + safeIp;

        long now = Instant.now().getEpochSecond();
        WindowCounter counter = counters.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStartEpoch() >= windowSeconds) {
                return new WindowCounter(now, 1);
            }

            return new WindowCounter(existing.windowStartEpoch(), existing.count() + 1);
        });

        if (counter.count() > maxRequestsPerWindow) {
            throw new TooManyRequestsException("Too many requests for " + action + ". Please retry later.");
        }
    }

    private record WindowCounter(long windowStartEpoch, int count) {
    }
}
