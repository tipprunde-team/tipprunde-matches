package com.mosbach.tipprunde.matches.client;

import com.mosbach.tipprunde.matches.client.dto.FdCompetition;
import com.mosbach.tipprunde.matches.client.dto.FdMatchesResponse;

// KI-Log AH-13 "Baue den Match-Service, der Spiele von football-data.org holt, mit Ratenlimit und Wiederholung."
// Zugriff auf football-data.org v4; drei Endpunkte für Wettbewerb, Spieltag und ganze Saison
public interface FootballDataClient {

    FdCompetition getCompetition();

    FdMatchesResponse getMatchesOfMatchday(int matchday);

    FdMatchesResponse getSeasonMatches();
}
