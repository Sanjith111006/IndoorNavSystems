package com.indoornav.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.indoornav.backend.config.AppProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginThrottleTest {

    private static final String ADDRESS = "203.0.113.5";

    private MutableClock clock;
    private LoginThrottle throttle;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-03-01T10:00:00Z"));
        AppProperties properties = new AppProperties(
                false,
                new AppProperties.Admin("admin", ""),
                new AppProperties.Jwt("", 60),
                new AppProperties.Login(5, 15, false),
                new AppProperties.FloorPlans(""));
        throttle = new LoginThrottle(clock, properties);
    }

    @Test
    void allowsAttemptsBelowTheLimit() {
        fail(4);

        assertThat(throttle.retryAfter(ADDRESS)).isEmpty();
    }

    @Test
    void blocksAtTheLimitUntilTheWindowEnds() {
        fail(5);
        clock.advance(Duration.ofMinutes(5));

        assertThat(throttle.retryAfter(ADDRESS)).contains(Duration.ofMinutes(10));
    }

    @Test
    void allowsAgainOnceTheWindowHasPassed() {
        fail(5);
        clock.advance(Duration.ofMinutes(15));

        assertThat(throttle.retryAfter(ADDRESS)).isEmpty();
    }

    @Test
    void anExpiredWindowStartsCountingAfresh() {
        fail(5);
        clock.advance(Duration.ofMinutes(16));
        fail(1);

        assertThat(throttle.retryAfter(ADDRESS)).isEmpty();
    }

    @Test
    void addressesAreCountedSeparately() {
        fail(5);

        assertThat(throttle.retryAfter("198.51.100.9")).isEmpty();
    }

    @Test
    void clearingRemovesTheFailures() {
        fail(5);
        throttle.clear(ADDRESS);

        assertThat(throttle.retryAfter(ADDRESS)).isEmpty();
    }

    private void fail(int times) {
        for (int attempt = 0; attempt < times; attempt++) {
            throttle.recordFailure(ADDRESS);
        }
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
