package com.mosbach.tipprunde.matches.sync;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mosbach.tipprunde.matches.client.FootballDataClient;
import com.mosbach.tipprunde.matches.client.dto.FdCompetition;
import com.mosbach.tipprunde.matches.client.dto.FdMatch;
import com.mosbach.tipprunde.matches.domain.MatchMapper;
import com.mosbach.tipprunde.matches.domain.MatchStatus;
import com.mosbach.tipprunde.matches.domain.MatchUpdated;

// Abgleich: holt Spiele, bildet sie ab und gibt jedes Spiel als Event weiter; der Core verarbeitet idempotent
@Service
public class MatchSyncService {

    private static final Logger log = LoggerFactory.getLogger(MatchSyncService.class);

    private final FootballDataClient client;
    private final MatchEventPublisher publisher;
    private final Clock clock;

    public MatchSyncService(FootballDataClient client, MatchEventPublisher publisher, Clock clock) {
        this.client = client;
        this.publisher = publisher;
        this.clock = clock;
    }

    // Kurzer Abgleich (alle 10 Minuten): aktueller und vorheriger Spieltag, weil Spiele des
    // alten Spieltags noch laufen oder nachträglich enden können, wenn football-data.org schon umgeschaltet hat
    public int syncCurrentMatchdays() {
        FdCompetition competition = client.getCompetition();
        Integer current = competition == null || competition.currentSeason() == null ? null
                : competition.currentSeason().currentMatchday();
        if (current == null) {
            log.warn("Sync skipped: current matchday unknown");
            return 0;
        }
        int count = 0;
        if (current > 1) {
            count += publishAll("matchday " + (current - 1), client.getMatchesOfMatchday(current - 1).matchesOrEmpty());
        }
        count += publishAll("matchday " + current, client.getMatchesOfMatchday(current).matchesOrEmpty());
        return count;
    }

    // Ganzer Spielplan (einmal täglich), übernimmt Verschiebungen und neue Anstoßzeiten
    public int syncSeason() {
        return publishAll("season", client.getSeasonMatches().matchesOrEmpty());
    }

    private int publishAll(String scope, List<FdMatch> matches) {
        Instant now = clock.instant();
        int finished = 0;
        for (FdMatch match : matches) {
            MatchUpdated event = MatchMapper.toEvent(match, now);
            publisher.publish(event);
            if (event.status() == MatchStatus.FINISHED) {
                finished++;
            }
        }
        log.info("Sync {}: {} matches published, {} finished", scope, matches.size(), finished);
        return matches.size();
    }
}
