package com.mosbach.tipprunde.matches.sync;

import com.mosbach.tipprunde.matches.domain.MatchUpdated;

// KI-Log AH-13 "Baue den Match-Service, der Spiele von football-data.org holt, mit Ratenlimit und Wiederholung."
// Ziel der Events: mit QUEUE_URL die Queue (AmqpMatchEventPublisher), sonst nur das Log
public interface MatchEventPublisher {

    void publish(MatchUpdated event);
}
