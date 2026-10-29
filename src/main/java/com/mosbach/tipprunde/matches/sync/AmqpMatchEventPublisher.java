package com.mosbach.tipprunde.matches.sync;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import com.mosbach.tipprunde.matches.config.QueueProperties;
import com.mosbach.tipprunde.matches.domain.MatchUpdated;

import tools.jackson.databind.json.JsonMapper;

// Schickt jedes Event als JSON in die Queue tipprunde.match-events; aktiv nur mit QUEUE_URL
@Component
@ConditionalOnExpression("!'${spring.rabbitmq.addresses:}'.isEmpty()")
public class AmqpMatchEventPublisher implements MatchEventPublisher {

    // Default-Exchange: Routing-Key ist der Name der Queue (Point-to-Point Channel)
    private static final String DEFAULT_EXCHANGE = "";

    private final RabbitOperations rabbit;
    private final JsonMapper jsonMapper;
    private final String queueName;
    private final long confirmTimeoutMillis;

    public AmqpMatchEventPublisher(RabbitOperations rabbit, JsonMapper jsonMapper, QueueProperties properties) {
        this.rabbit = rabbit;
        this.jsonMapper = jsonMapper;
        this.queueName = properties.name();
        this.confirmTimeoutMillis = properties.confirmTimeout().toMillis();
    }

    @Override
    public void publish(MatchUpdated event) {
        Message message = new Message(jsonMapper.writeValueAsBytes(event), properties(event));
        // Senden und auf die Bestätigung des Brokers warten; ohne Bestätigung bricht der Job mit Fehler ab
        rabbit.invoke(ops -> {
            ops.send(DEFAULT_EXCHANGE, queueName, message);
            ops.waitForConfirmsOrDie(confirmTimeoutMillis);
            return null;
        });
    }

    // Persistent, damit die Nachricht einen Neustart des Brokers übersteht, solange der Core schläft
    static MessageProperties properties(MatchUpdated event) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding("UTF-8");
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setType(event.type());
        properties.setMessageId(event.externalId() + "@" + event.occurredAt());
        return properties;
    }
}
