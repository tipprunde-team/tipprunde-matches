package com.mosbach.tipprunde.matches.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Einstellungen der Queue aus application.properties; die Verbindung selbst kommt nur aus QUEUE_URL
@ConfigurationProperties(prefix = "app.queue")
public record QueueProperties(
        String name,
        Duration confirmTimeout) {
}
