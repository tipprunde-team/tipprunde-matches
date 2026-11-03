package com.mosbach.tipprunde.matches.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// KI-Log AH-13 "Baue den Match-Service, der Spiele von football-data.org holt, mit Ratenlimit und Wiederholung."
@JsonIgnoreProperties(ignoreUnknown = true)
public record FdSeason(Integer currentMatchday) {
}
