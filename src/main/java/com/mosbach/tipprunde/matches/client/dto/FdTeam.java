package com.mosbach.tipprunde.matches.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FdTeam(String name, String shortName, String tla, String crest) {
}
