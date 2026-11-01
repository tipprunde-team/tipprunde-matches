package com.mosbach.tipprunde.matches.config;

import java.net.http.HttpClient;
import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.mosbach.tipprunde.matches.client.RetryingRequestExecutor;

// HTTP-Client für football-data.org mit Timeouts, dazu Uhr und Wartefunktion (in Tests austauschbar)
@Configuration
public class ClientConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public RestClient footballDataRestClient(FootballDataProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        // Standard-Builder; Jackson wird automatisch erkannt und liest das JSON
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    public RetryingRequestExecutor retryingRequestExecutor(FootballDataProperties properties, Clock clock) {
        return new RetryingRequestExecutor(properties.minInterval(), properties.maxAttempts(), clock,
                RetryingRequestExecutor.Sleeper.REAL);
    }
}
