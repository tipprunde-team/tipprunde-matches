package com.mosbach.tipprunde.matches.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.mosbach.tipprunde.matches.client.dto.FdCompetition;
import com.mosbach.tipprunde.matches.client.dto.FdMatchesResponse;
import com.mosbach.tipprunde.matches.config.FootballDataProperties;

// Echte Aufrufe über HTTP; jeder Aufruf läuft über Ratenlimit und Wiederholung
@Component
public class HttpFootballDataClient implements FootballDataClient {

    private static final String TOKEN_HEADER = "X-Auth-Token";

    private final RestClient restClient;
    private final RetryingRequestExecutor executor;
    private final FootballDataProperties properties;

    public HttpFootballDataClient(RestClient footballDataRestClient, RetryingRequestExecutor executor,
            FootballDataProperties properties) {
        this.restClient = footballDataRestClient;
        this.executor = executor;
        this.properties = properties;
    }

    @Override
    public FdCompetition getCompetition() {
        return executor.execute("GET /competitions/" + properties.competition(), () -> restClient.get()
                .uri("/competitions/{code}", properties.competition())
                .header(TOKEN_HEADER, token())
                .retrieve()
                .body(FdCompetition.class));
    }

    @Override
    public FdMatchesResponse getMatchesOfMatchday(int matchday) {
        return executor.execute("GET /matches?matchday=" + matchday, () -> restClient.get()
                .uri("/competitions/{code}/matches?matchday={matchday}", properties.competition(), matchday)
                .header(TOKEN_HEADER, token())
                .retrieve()
                .body(FdMatchesResponse.class));
    }

    @Override
    public FdMatchesResponse getSeasonMatches() {
        return executor.execute("GET /matches (season)", () -> restClient.get()
                .uri("/competitions/{code}/matches", properties.competition())
                .header(TOKEN_HEADER, token())
                .retrieve()
                .body(FdMatchesResponse.class));
    }

    // Token nur aus der Umgebung; ohne Token bricht der Abgleich mit klarer Meldung ab
    private String token() {
        String token = properties.token();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("FOOTBALL_DATA_TOKEN is not set");
        }
        return token;
    }
}
