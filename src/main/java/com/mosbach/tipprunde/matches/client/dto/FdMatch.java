package com.mosbach.tipprunde.matches.client.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Ein Spiel aus football-data.org v4, nur die Felder, die Tipprunde braucht
@JsonIgnoreProperties(ignoreUnknown = true)
public record FdMatch(long id, Instant utcDate, String status, Integer matchday, FdTeam homeTeam,
        FdTeam awayTeam, FdScore score) {
}
