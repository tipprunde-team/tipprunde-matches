package com.mosbach.tipprunde.matches.client;

import com.mosbach.tipprunde.matches.client.dto.FdCompetition;
import com.mosbach.tipprunde.matches.client.dto.FdMatchesResponse;

// Zugriff auf football-data.org v4; drei Endpunkte für Wettbewerb, Spieltag und ganze Saison
public interface FootballDataClient {

    FdCompetition getCompetition();

    FdMatchesResponse getMatchesOfMatchday(int matchday);

    FdMatchesResponse getSeasonMatches();
}
