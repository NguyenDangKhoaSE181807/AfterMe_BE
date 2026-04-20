package com.example.reminder.service.security;

public interface RequestRateLimiter {

    void checkRateLimit(String action, String actorId, String ipAddress);
}
