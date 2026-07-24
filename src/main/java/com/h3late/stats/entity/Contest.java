package com.h3late.stats.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "contest")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContestType type;

    @Column(nullable = false)
    private Instant startDate;

    @Column(nullable = false)
    private Instant endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContestStatus status;

    @Column(nullable = false)
    private int dailyVoteBudget;

    @Column(nullable = false)
    private int maxClipDurationSeconds;

    @Column(nullable = false)
    private int maxSubmissionsPerUser;

    // "DAILY" or comma-separated day abbreviations e.g. "MON,THU"
    @Column(nullable = false)
    private String voteRefreshSchedule;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
