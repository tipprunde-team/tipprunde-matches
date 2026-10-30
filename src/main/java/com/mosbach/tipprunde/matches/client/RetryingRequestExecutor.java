package com.mosbach.tipprunde.matches.client;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

// Ratenlimit und Wiederholung für football-data.org: höchstens 10 Anfragen pro Minute (Free-Tarif),
// bei 429 bis zum Zurücksetzen des Zählers warten, bei Timeout oder 5xx mit wachsender Wartezeit wiederholen
public class RetryingRequestExecutor {

    private static final Logger log = LoggerFactory.getLogger(RetryingRequestExecutor.class);

    static final String RESET_HEADER = "X-RequestCounter-Reset";
    static final Duration DEFAULT_RESET_WAIT = Duration.ofSeconds(60);
    static final Duration MAX_RESET_WAIT = Duration.ofSeconds(65);
    static final Duration BACKOFF_STEP = Duration.ofSeconds(5);

    // Wartefunktion als Interface, damit Tests nicht wirklich warten
    @FunctionalInterface
    public interface Sleeper {
        void sleep(Duration duration);

        Sleeper REAL = duration -> {
            try {
                Thread.sleep(duration.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting", e);
            }
        };
    }

    private final Duration minInterval;
    private final int maxAttempts;
    private final Clock clock;
    private final Sleeper sleeper;
    private Instant lastRequestAt;

    public RetryingRequestExecutor(Duration minInterval, int maxAttempts, Clock clock, Sleeper sleeper) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        this.minInterval = minInterval;
        this.maxAttempts = maxAttempts;
        this.clock = clock;
        this.sleeper = sleeper;
    }

    public synchronized <T> T execute(String description, Supplier<T> call) {
        for (int attempt = 1; ; attempt++) {
            throttle();
            lastRequestAt = clock.instant();
            try {
                T result = call.get();
                log.info("football-data {}: OK (attempt {})", description, attempt);
                return result;
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() != HttpStatus.TOO_MANY_REQUESTS.value() || attempt >= maxAttempts) {
                    log.warn("football-data {}: status {} (attempt {}), giving up", description,
                            e.getStatusCode().value(), attempt);
                    throw e;
                }
                Duration wait = resetWait(e);
                log.warn("football-data {}: status 429 (attempt {}), waiting {} s", description, attempt,
                        wait.toSeconds());
                sleeper.sleep(wait);
            } catch (HttpServerErrorException | ResourceAccessException e) {
                if (attempt >= maxAttempts) {
                    log.warn("football-data {}: {} (attempt {}), giving up", description, reason(e), attempt);
                    throw e;
                }
                Duration wait = BACKOFF_STEP.multipliedBy(attempt);
                log.warn("football-data {}: {} (attempt {}), waiting {} s", description, reason(e), attempt,
                        wait.toSeconds());
                sleeper.sleep(wait);
            }
        }
    }

    // Abstand zwischen zwei Anfragen einhalten (60 s / 10 Anfragen = 6 s)
    private void throttle() {
        if (lastRequestAt == null) {
            return;
        }
        Duration sinceLast = Duration.between(lastRequestAt, clock.instant());
        Duration wait = minInterval.minus(sinceLast);
        if (!wait.isNegative() && !wait.isZero()) {
            sleeper.sleep(wait);
        }
    }

    // football-data.org nennt im Header die Sekunden bis zum Zurücksetzen des Zählers
    static Duration resetWait(HttpClientErrorException e) {
        String header = e.getResponseHeaders() == null ? null : e.getResponseHeaders().getFirst(RESET_HEADER);
        if (header == null) {
            return DEFAULT_RESET_WAIT;
        }
        try {
            Duration wait = Duration.ofSeconds(Long.parseLong(header.trim()) + 1);
            return wait.compareTo(MAX_RESET_WAIT) > 0 ? MAX_RESET_WAIT : wait;
        } catch (NumberFormatException ex) {
            return DEFAULT_RESET_WAIT;
        }
    }

    private static String reason(RuntimeException e) {
        if (e instanceof HttpServerErrorException server) {
            return "status " + server.getStatusCode().value();
        }
        return "timeout or connection error";
    }
}
