package com.mosbach.tipprunde.matches.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpTimeoutException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.core.RabbitOperations.OperationsCallback;

import com.mosbach.tipprunde.matches.config.QueueProperties;
import com.mosbach.tipprunde.matches.domain.MatchStatus;
import com.mosbach.tipprunde.matches.domain.MatchUpdated;
import com.mosbach.tipprunde.matches.domain.Score;
import com.mosbach.tipprunde.matches.domain.Team;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// Senden ohne echte Queue: Format laut Vertrag D.2 (dieselben Beispieldateien wie im Core), Routing und Bestätigung
class AmqpMatchEventPublisherTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final RabbitOperations rabbit = mock(RabbitOperations.class);
    private final RabbitOperations channel = mock(RabbitOperations.class);
    private final AmqpMatchEventPublisher publisher =
            new AmqpMatchEventPublisher(rabbit, JSON,
                    new QueueProperties("tipprunde.match-events", Duration.ofSeconds(5)));

    AmqpMatchEventPublisherTest() {
        // invoke führt den Block auf einem Kanal aus; hier mit einem zweiten Mock als Kanal
        when(rabbit.invoke(any())).thenAnswer(call -> {
            OperationsCallback<?> callback = call.getArgument(0);
            return callback.doInRabbit(channel);
        });
    }

    private static JsonNode contract(String file) throws IOException {
        try (InputStream in = AmqpMatchEventPublisherTest.class.getResourceAsStream("/contract/" + file)) {
            return JSON.readTree(in);
        }
    }

    private Message sent() {
        ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
        verify(channel).send(eq(""), eq("tipprunde.match-events"), message.capture());
        return message.getValue();
    }

    private static MatchUpdated finished() {
        return new MatchUpdated(MatchUpdated.TYPE, 565833, 7, Instant.parse("2026-10-23T18:30:00Z"),
                MatchStatus.FINISHED,
                new Team("VfB Stuttgart", "Stuttgart", "https://crests.football-data.org/10.png"),
                new Team("Borussia Mönchengladbach", "M'gladbach", "https://crests.football-data.org/18.png"),
                new Score(2, 3), Instant.parse("2026-10-28T22:03:45.109Z"));
    }

    @Test
    void finishedEventMatchesContractOfCore() throws IOException {
        publisher.publish(finished());

        assertEquals(contract("match-updated-finished.json"), JSON.readTree(sent().getBody()));
    }

    @Test
    void upcomingEventHasScoreNull() throws IOException {
        JsonNode expected = contract("match-updated-upcoming.json");
        MatchUpdated upcoming = new MatchUpdated(MatchUpdated.TYPE, expected.get("externalId").asLong(),
                expected.get("matchday").asInt(), Instant.parse(expected.get("kickoffUtc").asString()),
                MatchStatus.UPCOMING, team(expected.get("homeTeam")), team(expected.get("awayTeam")), null,
                Instant.parse(expected.get("occurredAt").asString()));

        publisher.publish(upcoming);

        assertEquals(expected, JSON.readTree(sent().getBody()));
    }

    @Test
    void messageIsPersistentJsonAndConfirmed() {
        publisher.publish(finished());

        Message message = sent();
        assertEquals("application/json", message.getMessageProperties().getContentType());
        assertEquals(MessageDeliveryMode.PERSISTENT, message.getMessageProperties().getDeliveryMode());
        assertEquals("MATCH_UPDATED", message.getMessageProperties().getType());
        assertEquals("565833@2026-10-28T22:03:45.109Z", message.getMessageProperties().getMessageId());
        verify(channel).waitForConfirmsOrDie(5000);
    }

    @Test
    void missingConfirmFailsThePublish() {
        doThrow(new AmqpTimeoutException("no confirm")).when(channel).waitForConfirmsOrDie(anyLong());

        assertThrows(AmqpTimeoutException.class, () -> publisher.publish(finished()));
    }

    private static Team team(JsonNode node) {
        return new Team(node.get("name").asString(), node.get("shortName").asString(), node.get("crest").asString());
    }
}
