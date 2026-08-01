package com.h3late.stats.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "contest_clip")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContestClip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long contestId;

    @Column(nullable = false)
    private String videoId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int startSeconds;

    @Column(nullable = false)
    private int endSeconds;

    @Column(name = "submitter_token", nullable = false)
    private String userToken;

    // Populated at write time for logged-in submitters (fast "my history" lookups). The
    // submitter_token column above remains the source of truth for per-user limit enforcement.
    private Long userId;

    @Column(nullable = false)
    private String submitterName;

    @Column(nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(nullable = false)
    @Builder.Default
    private int voteCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean removed = false;

    private Instant removedAt;

    private String removedReason;

    @PrePersist
    void prePersist() {
        submittedAt = Instant.now();
    }
}
