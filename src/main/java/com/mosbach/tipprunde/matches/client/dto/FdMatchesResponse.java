package com.mosbach.tipprunde.matches.client.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// KI-Log AH-13 "Baue den Match-Service, der Spiele von football-data.org holt, mit Ratenlimit und Wiederholung."
// Antwort von GET /competitions/BL1/matches
@JsonIgnoreProperties(ignoreUnknown = true)
public record FdMatchesResponse(List<FdMatch> matches) {

    public List<FdMatch> matchesOrEmpty() {
        return matches == null ? List.of() : matches;
    }
}
