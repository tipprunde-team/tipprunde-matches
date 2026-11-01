package com.mosbach.tipprunde.matches.domain;

import java.time.Instant;

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
