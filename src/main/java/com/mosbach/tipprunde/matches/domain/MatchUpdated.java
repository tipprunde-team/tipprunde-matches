package com.mosbach.tipprunde.matches.domain;

import java.time.Instant;

// KI-Log AH-13 "Baue den Match-Service, der Spiele von football-data.org holt, mit Ratenlimit und Wiederholung."
// Event MATCH_UPDATED für die Queue tipprunde.match-events (Vertrag aus Projektplan D.2)
public record MatchUpdated(
        String type,
        long externalId,
        int matchday,
        Instant kickoffUtc,
        MatchStatus status,
        Team homeTeam,
        Team awayTeam,
        Score score,
        Instant occurredAt) {

    public static final String TYPE = "MATCH_UPDATED";
}
