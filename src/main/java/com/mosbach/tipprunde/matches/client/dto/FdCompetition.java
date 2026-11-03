package com.mosbach.tipprunde.matches.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// KI-Log AH-13 "Baue den Match-Service, der Spiele von football-data.org holt, mit Ratenlimit und Wiederholung."
// Antwort von GET /competitions/BL1, gebraucht wird nur der aktuelle Spieltag
@JsonIgnoreProperties(ignoreUnknown = true)
public record FdCompetition(String code, FdSeason currentSeason) {
}
