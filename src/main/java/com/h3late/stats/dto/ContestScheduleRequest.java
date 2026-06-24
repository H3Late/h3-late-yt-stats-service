package com.h3late.stats.dto;

import com.h3late.stats.entity.ContestType;
import lombok.Data;

import java.time.Instant;

@Data
public class ContestScheduleRequest {
    private ContestType type;
    private Instant scheduledStartAt;
    private int durationDays;
    private int dailyVoteBudget;
    private int maxClipDurationSeconds;
    private int maxSubmissionsPerUser;
    // "DAILY" or comma-separated abbreviations e.g. "MON,THU"
    private String voteRefreshSchedule;
}
