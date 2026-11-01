package com.mosbach.tipprunde.matches.client.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Antwort von GET /competitions/BL1/matches
@JsonIgnoreProperties(ignoreUnknown = true)
public record FdMatchesResponse(List<FdMatch> matches) {

    public List<FdMatch> matchesOrEmpty() {
        return matches == null ? List.of() : matches;
    }
}
