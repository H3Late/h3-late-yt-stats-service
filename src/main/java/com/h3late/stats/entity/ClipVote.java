package com.h3late.stats.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "clip_vote",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_clip_vote_per_period",
        columnNames = {"clip_id", "voter_token", "vote_period_start"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClipVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clip_id", nullable = false)
    private Long clipId;

    // Denormalized from clip for efficient budget queries without joins
    @Column(nullable = false)
    private Long contestId;

    @Column(name = "voter_token", nullable = false)
    private String userToken;

    // Populated at write time for logged-in voters (fast "my history" lookups). The
    // voter_token column above remains the source of truth for per-user limit enforcement.
    private Long userId;

    // UTC midnight of the vote period start — shifts when voteRefreshSchedule ticks over
    @Column(nullable = false)
    private Instant votePeriodStart;

    @Column(nullable = false, updatable = false)
    private Instant votedAt;

    @PrePersist
    void prePersist() {
        votedAt = Instant.now();
    }
}
