package com.mosbach.tipprunde.matches.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.mosbach.tipprunde.matches.client.FootballDataClient;
import com.mosbach.tipprunde.matches.client.dto.FdCompetition;
import com.mosbach.tipprunde.matches.client.dto.FdMatch;
import com.mosbach.tipprunde.matches.client.dto.FdMatchesResponse;
import com.mosbach.tipprunde.matches.client.dto.FdSeason;
import com.mosbach.tipprunde.matches.domain.MatchStatus;
import com.mosbach.tipprunde.matches.domain.MatchUpdated;

import tools.jackson.databind.json.JsonMapper;

// Abgleich mit echten Beispielantworten von football-data.org (Spieltag 4 beendet, Spieltag 7 angesetzt)
class MatchSyncServiceTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Instant NOW = Instant.parse("2026-10-29T10:00:00Z");

    private final List<MatchUpdated> published = new ArrayList<>();
    private final List<Integer> requestedMatchdays = new ArrayList<>();

    private static FdMatchesResponse sample(String file) throws IOException {
        try (InputStream in = MatchSyncServiceTest.class.getResourceAsStream("/football-data/" + file)) {
            return JSON.readValue(in, FdMatchesResponse.class);
        }
    }

    private MatchSyncService service(int currentMatchday) throws IOException {
        FdMatchesResponse finished = sample("bl1-matchday-4-finished.json");
        FdMatchesResponse upcoming = sample("bl1-matchday-7.json");
        FootballDataClient client = new FootballDataClient() {
            @Override
            public FdCompetition getCompetition() {
                return new FdCompetition("BL1", new FdSeason(currentMatchday));
            }

            @Override
            public FdMatchesResponse getMatchesOfMatchday(int matchday) {
                requestedMatchdays.add(matchday);
                return matchday == 4 ? finished : matchday == 7 ? upcoming : new FdMatchesResponse(List.of());
            }

            @Override
            public FdMatchesResponse getSeasonMatches() {
                List<FdMatch> all = new ArrayList<>(finished.matches());
                all.addAll(upcoming.matches());
                return new FdMatchesResponse(all);
            }
        };
        return new MatchSyncService(client, published::add, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void sampleResponsesAreParsed() throws IOException {
        assertEquals(9, sample("bl1-matchday-4-finished.json").matches().size());
        assertEquals(9, sample("bl1-matchday-7.json").matches().size());
    }

    @Test
    void currentAndPreviousMatchdayArePublished() throws IOException {
        int count = service(5).syncCurrentMatchdays();

        assertEquals(List.of(4, 5), requestedMatchdays);
        assertEquals(9, count);
        assertEquals(9, published.size());
        assertTrue(published.stream().allMatch(e -> e.status() == MatchStatus.FINISHED && e.score() != null));
        assertTrue(published.stream().allMatch(e -> e.matchday() == 4 && NOW.equals(e.occurredAt())));
    }

    @Test
    void upcomingMatchesHaveNoScore() throws IOException {
        service(7).syncCurrentMatchdays();

        assertEquals(List.of(6, 7), requestedMatchdays);
        assertEquals(9, published.size());
        MatchUpdated first = published.get(0);
        assertEquals(MatchStatus.UPCOMING, first.status());
        assertNull(first.score());
        assertEquals("Stuttgart", first.homeTeam().shortName());
        assertEquals(Instant.parse("2026-10-23T18:30:00Z"), first.kickoffUtc());
    }

    @Test
    void firstMatchdayHasNoPreviousMatchday() throws IOException {
        service(1).syncCurrentMatchdays();
        assertEquals(List.of(1), requestedMatchdays);
    }

    @Test
    void seasonSyncPublishesAllMatches() throws IOException {
        assertEquals(18, service(7).syncSeason());
        assertEquals(18, published.size());
    }
}
