package com.mosbach.tipprunde.matches.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.mosbach.tipprunde.matches.client.dto.FdMatchesResponse;
import com.mosbach.tipprunde.matches.config.ClientConfig;
import com.mosbach.tipprunde.matches.config.FootballDataProperties;
import com.sun.net.httpserver.HttpServer;

// Echter HTTP-Aufruf gegen einen lokalen Ersatz-Server statt football-data.org: Pfade, Token-Header,
// JSON-Abbildung und Wiederholung nach 429
class HttpFootballDataClientTest {

    private HttpServer server;
    private final List<String> requests = new ArrayList<>();
    private final List<String> tokens = new ArrayList<>();
    private final AtomicInteger tooManyRequestsLeft = new AtomicInteger();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v4/competitions/BL1", exchange -> {
            String uri = exchange.getRequestURI().toString();
            requests.add(uri);
            tokens.add(exchange.getRequestHeaders().getFirst("X-Auth-Token"));
            if (tooManyRequestsLeft.getAndDecrement() > 0) {
                exchange.getResponseHeaders().add("X-RequestCounter-Reset", "3");
                exchange.sendResponseHeaders(429, -1);
                exchange.close();
                return;
            }
            byte[] body = uri.contains("matchday=7") ? sample("bl1-matchday-7.json")
                    : "{\"code\":\"BL1\",\"currentSeason\":{\"currentMatchday\":7}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private static byte[] sample(String file) throws IOException {
        try (InputStream in = HttpFootballDataClientTest.class.getResourceAsStream("/football-data/" + file)) {
            return in.readAllBytes();
        }
    }

    private HttpFootballDataClient client(String token) {
        FootballDataProperties properties = new FootballDataProperties(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v4", token, "BL1",
                Duration.ofSeconds(6), 3, Duration.ofSeconds(2), Duration.ofSeconds(2));
        RestClient restClient = new ClientConfig().footballDataRestClient(properties);
        RetryingRequestExecutor executor = new RetryingRequestExecutor(properties.minInterval(), 3,
                Clock.systemUTC(), duration -> { });
        return new HttpFootballDataClient(restClient, executor, properties);
    }

    @Test
    void readsCurrentMatchdayAndSendsToken() {
        assertEquals(7, client("test-token").getCompetition().currentSeason().currentMatchday());
        assertEquals(List.of("/v4/competitions/BL1"), requests);
        assertEquals(List.of("test-token"), tokens);
    }

    @Test
    void readsMatchesOfAMatchday() {
        FdMatchesResponse response = client("test-token").getMatchesOfMatchday(7);
        assertEquals("/v4/competitions/BL1/matches?matchday=7", requests.get(0));
        assertEquals(9, response.matches().size());
        assertEquals("VfB Stuttgart", response.matches().get(0).homeTeam().name());
    }

    @Test
    void retriesAfterTooManyRequests() {
        tooManyRequestsLeft.set(1);
        FdMatchesResponse response = client("test-token").getMatchesOfMatchday(7);
        assertEquals(2, requests.size());
        assertEquals(9, response.matches().size());
    }

    @Test
    void missingTokenStopsBeforeAnyRequest() {
        assertThrows(IllegalStateException.class, () -> client("").getCompetition());
        assertEquals(List.of(), requests);
    }
}
