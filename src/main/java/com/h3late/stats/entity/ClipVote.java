package com.h3late.stats.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "clip_vote",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_clip_vote_per_period",
        columnNames = {"clip_id", "user_id", "vote_period_start"}
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

    // Either a guest's raw UUID token or "u:<AppUser.id>" for a logged-in voter (see
    // AccountIdentity) — a String, not a real FK, since it has to hold both shapes in one column.
    @Column(nullable = false)
    private String userId;

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
