package com.mosbach.tipprunde.matches.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

// Ratenlimit und Wiederholung ohne echtes Warten: die Uhr springt, wenn der Sleeper "wartet"
class RetryingRequestExecutorTest {

    private Instant now = Instant.parse("2026-10-29T10:00:00Z");
    private final List<Duration> waits = new ArrayList<>();

    private final Clock clock = new Clock() {
        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    };

    private final RetryingRequestExecutor executor = new RetryingRequestExecutor(Duration.ofSeconds(6), 3, clock,
            duration -> {
                waits.add(duration);
                now = now.plus(duration);
            });

    private static HttpClientErrorException tooManyRequests(String resetSeconds) {
        HttpHeaders headers = new HttpHeaders();
        if (resetSeconds != null) {
            headers.add("X-RequestCounter-Reset", resetSeconds);
        }
        return HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", headers,
                new byte[0], StandardCharsets.UTF_8);
    }

    // Liefert nacheinander die vorgegebenen Ergebnisse oder wirft die vorgegebenen Fehler
    private static Supplier<String> sequence(List<Object> steps, int[] calls) {
        return () -> {
            Object step = steps.get(calls[0]++);
            if (step instanceof RuntimeException e) {
                throw e;
            }
            return (String) step;
        };
    }

    @Test
    void firstRequestDoesNotWait() {
        assertEquals("ok", executor.execute("test", () -> "ok"));
        assertEquals(List.of(), waits);
    }

    @Test
    void secondRequestKeepsSixSecondsDistance() {
        executor.execute("first", () -> "a");
        now = now.plusSeconds(2);
        executor.execute("second", () -> "b");
        assertEquals(List.of(Duration.ofSeconds(4)), waits);
    }

    @Test
    void tooManyRequestsWaitsUntilCounterResetAndRetries() {
        int[] calls = {0};
        String result = executor.execute("test", sequence(List.of(tooManyRequests("13"), "ok"), calls));
        assertEquals("ok", result);
        assertEquals(2, calls[0]);
        assertEquals(Duration.ofSeconds(14), waits.get(0));
    }

    @Test
    void tooManyRequestsWithoutHeaderWaitsOneMinute() {
        int[] calls = {0};
        executor.execute("test", sequence(List.of(tooManyRequests(null), "ok"), calls));
        assertEquals(Duration.ofSeconds(60), waits.get(0));
    }

    @Test
    void timeoutAndServerErrorAreRetriedWithGrowingWait() {
        int[] calls = {0};
        String result = executor.execute("test", sequence(List.of(
                new ResourceAccessException("Read timed out"),
                HttpServerErrorException.create(HttpStatus.BAD_GATEWAY, "Bad Gateway", new HttpHeaders(), new byte[0],
                        StandardCharsets.UTF_8),
                "ok"), calls));
        assertEquals("ok", result);
        assertEquals(3, calls[0]);
        // 5 s Backoff, dann fehlt noch 1 s bis zum 6-s-Abstand; 10 s Backoff deckt den Abstand schon ab
        assertEquals(List.of(Duration.ofSeconds(5), Duration.ofSeconds(1), Duration.ofSeconds(10)), waits);
    }

    @Test
    void givesUpAfterThreeAttempts() {
        int[] calls = {0};
        assertThrows(ResourceAccessException.class, () -> executor.execute("test", sequence(List.of(
                new ResourceAccessException("t1"), new ResourceAccessException("t2"),
                new ResourceAccessException("t3")), calls)));
        assertEquals(3, calls[0]);
    }

    @Test
    void otherClientErrorsAreNotRetried() {
        int[] calls = {0};
        HttpClientErrorException forbidden = HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden",
                new HttpHeaders(), new byte[0], StandardCharsets.UTF_8);
        assertThrows(HttpClientErrorException.class,
                () -> executor.execute("test", sequence(List.of(forbidden, "never"), calls)));
        assertEquals(1, calls[0]);
    }
}
