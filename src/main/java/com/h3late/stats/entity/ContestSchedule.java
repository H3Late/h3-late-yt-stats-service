package com.h3late.stats.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "contest_schedule")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContestSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContestType type;

    @Column(nullable = false)
    private Instant scheduledStartAt;

    @Column(nullable = false)
    private int durationDays;

    @Column(nullable = false)
    private int dailyVoteBudget;

    @Column(nullable = false)
    private int maxClipDurationSeconds;

    @Column(nullable = false)
    private int maxSubmissionsPerUser;

    @Column(nullable = false)
    private String voteRefreshSchedule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleStatus status;

    // Set once the schedule is processed and a Contest is created
    private Long contestId;

    private String failureReason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        if (status == null) status = ScheduleStatus.PENDING;
    }
}
