package com.mosbach.tipprunde.matches.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
