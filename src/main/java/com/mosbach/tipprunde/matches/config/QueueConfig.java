package com.mosbach.tipprunde.matches.config;

import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Legt die Queue dauerhaft an, falls es sie noch nicht gibt; gleiche Einstellungen wie im Core,
// sonst lehnt RabbitMQ die zweite Deklaration ab. Ohne Queue würde der Broker die Nachrichten verwerfen.
@Configuration
@ConditionalOnExpression("!'${spring.rabbitmq.addresses:}'.isEmpty()")
public class QueueConfig {

    @Bean
    public Queue matchEventsQueue(QueueProperties properties) {
        return new Queue(properties.name(), true);
    }
}
