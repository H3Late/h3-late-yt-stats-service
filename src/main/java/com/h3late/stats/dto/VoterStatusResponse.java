package com.h3late.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
public class VoterStatusResponse {
    private int votesRemainingToday;
    private Instant nextPeriodStart;
    private List<Long> votedClipIds;
}
