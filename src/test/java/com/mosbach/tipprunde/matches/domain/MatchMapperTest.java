package com.mosbach.tipprunde.matches.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.mosbach.tipprunde.matches.client.dto.FdGoals;
import com.mosbach.tipprunde.matches.client.dto.FdMatch;
import com.mosbach.tipprunde.matches.client.dto.FdScore;
import com.mosbach.tipprunde.matches.client.dto.FdTeam;

// Statusabbildung und Ergebnis nach 90 Minuten (FA-40)
class MatchMapperTest {

    @ParameterizedTest
    @CsvSource({
            "SCHEDULED, UPCOMING", "TIMED, UPCOMING",
            "IN_PLAY, LIVE", "PAUSED, LIVE", "EXTRA_TIME, LIVE", "PENALTY_SHOOTOUT, LIVE",
            "FINISHED, FINISHED", "AWARDED, FINISHED",
            "POSTPONED, POSTPONED", "SUSPENDED, POSTPONED",
            "CANCELLED, CANCELLED",
            "SOMETHING_NEW, UPCOMING"})
    void mapsAllExternalStatusValues(String external, MatchStatus expected) {
        assertEquals(expected, MatchMapper.mapStatus(external));
    }

    @Test
    void missingStatusCountsAsUpcoming() {
        assertEquals(MatchStatus.UPCOMING, MatchMapper.mapStatus(null));
    }

    @Test
    void finishedMatchUsesFullTimeScore() {
        FdScore score = new FdScore("REGULAR", new FdGoals(2, 1), null);
        assertEquals(new Score(2, 1), MatchMapper.scoreAfter90Minutes(MatchStatus.FINISHED, score));
    }

    @Test
    void afterExtraTimeOnlyTheRegularTimeCounts() {
        FdScore score = new FdScore("EXTRA_TIME", new FdGoals(3, 2), new FdGoals(2, 2));
        assertEquals(new Score(2, 2), MatchMapper.scoreAfter90Minutes(MatchStatus.FINISHED, score));
    }

    @Test
    void runningOrUpcomingMatchHasNoScore() {
        FdScore score = new FdScore("REGULAR", new FdGoals(1, 0), null);
        assertNull(MatchMapper.scoreAfter90Minutes(MatchStatus.LIVE, score));
        assertNull(MatchMapper.scoreAfter90Minutes(MatchStatus.UPCOMING, new FdScore("REGULAR", new FdGoals(null, null), null)));
    }

    @Test
    void finishedMatchWithoutGoalsHasNoScore() {
        assertNull(MatchMapper.scoreAfter90Minutes(MatchStatus.FINISHED, new FdScore("REGULAR", new FdGoals(null, null), null)));
        assertNull(MatchMapper.scoreAfter90Minutes(MatchStatus.FINISHED, null));
    }

    @Test
    void eventContainsAllFieldsOfTheContract() {
        Instant kickoff = Instant.parse("2026-10-24T13:30:00Z");
        Instant now = Instant.parse("2026-10-24T15:25:10Z");
        FdMatch match = new FdMatch(565830, kickoff, "FINISHED", 7,
                new FdTeam("VfB Stuttgart", "Stuttgart", "VFB", "https://crests.football-data.org/10.png"),
                new FdTeam("Borussia Mönchengladbach", "M'gladbach", "BMG", "https://crests.football-data.org/18.png"),
                new FdScore("REGULAR", new FdGoals(2, 1), null));

        MatchUpdated event = MatchMapper.toEvent(match, now);

        assertEquals("MATCH_UPDATED", event.type());
        assertEquals(565830, event.externalId());
        assertEquals(7, event.matchday());
        assertEquals(kickoff, event.kickoffUtc());
        assertEquals(MatchStatus.FINISHED, event.status());
        assertEquals("Stuttgart", event.homeTeam().shortName());
        assertEquals("https://crests.football-data.org/18.png", event.awayTeam().crest());
        assertEquals(new Score(2, 1), event.score());
        assertEquals(now, event.occurredAt());
    }
}
