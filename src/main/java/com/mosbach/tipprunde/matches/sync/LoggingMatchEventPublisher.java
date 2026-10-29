package com.mosbach.tipprunde.matches.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import com.mosbach.tipprunde.matches.domain.MatchUpdated;

// Ohne QUEUE_URL (lokal, CI): jedes Event nur ins Log schreiben, damit der Abgleich trotzdem testbar bleibt
@Component
@ConditionalOnExpression("'${spring.rabbitmq.addresses:}'.isEmpty()")
public class LoggingMatchEventPublisher implements MatchEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingMatchEventPublisher.class);

    @Override
    public void publish(MatchUpdated event) {
        log.info("MATCH_UPDATED externalId={} matchday={} status={} score={}", event.externalId(), event.matchday(),
                event.status(), event.score() == null ? "-" : event.score().home() + ":" + event.score().away());
    }
}
