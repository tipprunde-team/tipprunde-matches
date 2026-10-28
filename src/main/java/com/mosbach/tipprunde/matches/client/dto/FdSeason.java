package com.mosbach.tipprunde.matches.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FdSeason(Integer currentMatchday) {
}
