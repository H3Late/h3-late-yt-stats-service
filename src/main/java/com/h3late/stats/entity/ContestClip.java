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

    // Either a guest's raw UUID token or "u:<AppUser.id>" for a logged-in submitter (see
    // AccountIdentity) — a String, not a real FK, since it has to hold both shapes in one column.
    @Column(nullable = false)
    private String userId;

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
