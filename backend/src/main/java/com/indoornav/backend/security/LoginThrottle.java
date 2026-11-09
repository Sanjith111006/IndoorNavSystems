package com.indoornav.backend.security;

import com.indoornav.backend.config.AppProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Counts failed logins per client address (API.md Section 11).
 *
 * <p>Counters live in memory and reset on restart. The window starts at the first failure and
 * expired entries are dropped on each failure, so the map cannot grow without bound.
 */
@Component
public class LoginThrottle {

    private record Attempts(int failures, Instant windowEnd) {
    }

    private final Map<String, Attempts> attemptsByAddress = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxFailedAttempts;
    private final Duration window;

    public LoginThrottle(Clock clock, AppProperties properties) {
        this.clock = clock;
        this.maxFailedAttempts = properties.login().maxFailedAttempts();
        this.window = Duration.ofMinutes(properties.login().attemptWindowMinutes());
    }

    /** Returns how long the address must wait, or empty if it may try now. */
    public Optional<Duration> retryAfter(String clientAddress) {
        Attempts attempts = attemptsByAddress.get(clientAddress);
        Instant now = clock.instant();
        if (attempts == null || attempts.failures() < maxFailedAttempts || !now.isBefore(attempts.windowEnd())) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(now, attempts.windowEnd()));
    }

    public void recordFailure(String clientAddress) {
        Instant now = clock.instant();
        attemptsByAddress.values().removeIf(attempts -> !now.isBefore(attempts.windowEnd()));
        attemptsByAddress.merge(
                clientAddress,
                new Attempts(1, now.plus(window)),
                (existing, first) -> new Attempts(existing.failures() + 1, existing.windowEnd()));
    }

    public void clear(String clientAddress) {
        attemptsByAddress.remove(clientAddress);
    }
}
