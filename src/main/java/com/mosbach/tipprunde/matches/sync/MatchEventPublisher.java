package com.mosbach.tipprunde.matches.sync;

import com.mosbach.tipprunde.matches.domain.MatchUpdated;

// Ziel der Events; bis zur Queue (F-AH-2) nur ins Log
public interface MatchEventPublisher {

    void publish(MatchUpdated event);
}
