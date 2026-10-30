package com.mosbach.tipprunde.matches.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// fullTime ist das Endergebnis; regularTime gibt es nur nach Verlängerung oder Elfmeterschießen
@JsonIgnoreProperties(ignoreUnknown = true)
public record FdScore(String duration, FdGoals fullTime, FdGoals regularTime) {
}
