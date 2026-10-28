package com.mosbach.tipprunde.matches.domain;

import java.time.Instant;

import com.mosbach.tipprunde.matches.client.dto.FdGoals;
import com.mosbach.tipprunde.matches.client.dto.FdMatch;
import com.mosbach.tipprunde.matches.client.dto.FdScore;
import com.mosbach.tipprunde.matches.client.dto.FdTeam;

// Reine Abbildung von football-data.org auf das eigene Modell, ohne Netz und ohne Spring (gut testbar)
public final class MatchMapper {

    private MatchMapper() {
    }

    // football-data.org v4 kennt mehr Statuswerte als Tipprunde; unbekannte Werte gelten als UPCOMING
    public static MatchStatus mapStatus(String externalStatus) {
        if (externalStatus == null) {
            return MatchStatus.UPCOMING;
        }
        return switch (externalStatus) {
            case "IN_PLAY", "PAUSED", "LIVE", "EXTRA_TIME", "PENALTY_SHOOTOUT" -> MatchStatus.LIVE;
            case "FINISHED", "AWARDED" -> MatchStatus.FINISHED;
            case "POSTPONED", "SUSPENDED" -> MatchStatus.POSTPONED;
            case "CANCELLED" -> MatchStatus.CANCELLED;
            default -> MatchStatus.UPCOMING; // SCHEDULED, TIMED
        };
    }

    // Ergebnis nur bei FINISHED; nach Verlängerung zählt regularTime, sonst fullTime (FA-40)
    public static Score scoreAfter90Minutes(MatchStatus status, FdScore score) {
        if (status != MatchStatus.FINISHED || score == null) {
            return null;
        }
        FdGoals goals = complete(score.regularTime()) ? score.regularTime() : score.fullTime();
        return complete(goals) ? new Score(goals.home(), goals.away()) : null;
    }

    public static MatchUpdated toEvent(FdMatch match, Instant occurredAt) {
        MatchStatus status = mapStatus(match.status());
        return new MatchUpdated(
                MatchUpdated.TYPE,
                match.id(),
                match.matchday() == null ? 0 : match.matchday(),
                match.utcDate(),
                status,
                team(match.homeTeam()),
                team(match.awayTeam()),
                scoreAfter90Minutes(status, match.score()),
                occurredAt);
    }

    private static boolean complete(FdGoals goals) {
        return goals != null && goals.home() != null && goals.away() != null;
    }

    private static Team team(FdTeam team) {
        if (team == null) {
            return null;
        }
        return new Team(team.name(), team.shortName(), team.crest());
    }
}
