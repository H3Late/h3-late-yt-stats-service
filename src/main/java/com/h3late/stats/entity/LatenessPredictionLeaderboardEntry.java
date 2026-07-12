package com.h3late.stats.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "latest_stream_leaderboard")
@Getter
public class LatenessPredictionLeaderboardEntry {
    @Id
    private String userName;
    private Integer userGuess;
    private Integer actualResult;
    private Integer proximityScore;
}
