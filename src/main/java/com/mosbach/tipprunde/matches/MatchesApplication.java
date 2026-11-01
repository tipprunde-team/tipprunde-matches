package com.mosbach.tipprunde.matches;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// Match-Service: holt Spielplan und Ergebnisse von football-data.org und meldet Änderungen über die Queue an den Core
@SpringBootApplication
@ConfigurationPropertiesScan
public class MatchesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatchesApplication.class, args);
    }
}
