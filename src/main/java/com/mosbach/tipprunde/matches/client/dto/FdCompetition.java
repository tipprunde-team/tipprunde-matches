package com.mosbach.tipprunde.matches.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Antwort von GET /competitions/BL1, gebraucht wird nur der aktuelle Spieltag
@JsonIgnoreProperties(ignoreUnknown = true)
public record FdCompetition(String code, FdSeason currentSeason) {
}
