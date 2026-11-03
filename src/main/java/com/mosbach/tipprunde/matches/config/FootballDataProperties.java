package com.mosbach.tipprunde.matches.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

// KI-Log AH-13 "Baue den Match-Service, der Spiele von football-data.org holt, mit Ratenlimit und Wiederholung."
// Einstellungen für football-data.org aus application.properties; der Token kommt nur aus FOOTBALL_DATA_TOKEN
@ConfigurationProperties(prefix = "app.football-data")
public record FootballDataProperties(
        String baseUrl,
        String token,
        String competition,
        Duration minInterval,
        int maxAttempts,
        Duration connectTimeout,
        Duration readTimeout) {
}
