package com.mosbach.tipprunde.matches.domain;

// KI-Log AH-13 "Baue den Match-Service, der Spiele von football-data.org holt, mit Ratenlimit und Wiederholung."
// Statuswerte im Event-Vertrag (Projektplan D.2), gleiche Namen wie im Core
public enum MatchStatus {
    UPCOMING,
    LIVE,
    FINISHED,
    POSTPONED,
    CANCELLED
}
