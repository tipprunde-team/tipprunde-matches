package com.mosbach.tipprunde.matches.sync;

import com.mosbach.tipprunde.matches.domain.MatchUpdated;

// Ziel der Events: mit QUEUE_URL die Queue (AmqpMatchEventPublisher), sonst nur das Log
public interface MatchEventPublisher {

    void publish(MatchUpdated event);
}
