package com.mosbach.tipprunde.matches.sync;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.mosbach.tipprunde.matches.config.QueueConfig;
import com.mosbach.tipprunde.matches.config.QueueProperties;

// Welcher Publisher aktiv ist, entscheidet nur QUEUE_URL; es wird keine Verbindung aufgebaut
class PublisherWiringTest {

    // Testwert ohne echtes Passwort, Port 1 ist absichtlich nicht erreichbar
    private static final String FAKE_URL = "amqps://testuser:testpass@localhost:1/testvhost";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, RabbitAutoConfiguration.class))
            .withUserConfiguration(Properties.class, QueueConfig.class, AmqpMatchEventPublisher.class,
                    LoggingMatchEventPublisher.class)
            .withPropertyValues("app.queue.name=tipprunde.match-events", "app.queue.confirm-timeout=5s",
                    "spring.rabbitmq.publisher-confirm-type=simple");

    @EnableConfigurationProperties(QueueProperties.class)
    static class Properties {
    }

    @Test
    void withoutQueueUrlEventsGoToTheLog() {
        runner.withPropertyValues("spring.rabbitmq.addresses=").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(MatchEventPublisher.class)).isInstanceOf(LoggingMatchEventPublisher.class);
            assertThat(context).doesNotHaveBean(Queue.class);
        });
    }

    @Test
    void withQueueUrlEventsGoToTheDurableQueue() {
        runner.withPropertyValues("spring.rabbitmq.addresses=" + FAKE_URL).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(MatchEventPublisher.class)).isInstanceOf(AmqpMatchEventPublisher.class);
            Queue queue = context.getBean(Queue.class);
            assertThat(queue.getName()).isEqualTo("tipprunde.match-events");
            assertThat(queue.isDurable()).isTrue();
        });
    }

    @Test
    void amqpsUrlUsesTlsAndPublisherConfirms() {
        runner.withPropertyValues("spring.rabbitmq.addresses=" + FAKE_URL).run(context -> {
            CachingConnectionFactory factory = context.getBean(CachingConnectionFactory.class);
            assertThat(factory.isSimplePublisherConfirms()).isTrue();
            assertThat(factory.getRabbitConnectionFactory().isSSL()).isTrue();
            assertThat(factory.getRabbitConnectionFactory().getVirtualHost()).isEqualTo("testvhost");
        });
    }
}
