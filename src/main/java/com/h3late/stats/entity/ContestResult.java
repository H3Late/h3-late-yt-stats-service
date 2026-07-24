package com.h3late.stats.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "contest_result")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long contestId;

    // 1 = gold, 2 = silver, 3 = bronze
    @Column(nullable = false)
    private int rank;

    @Column(nullable = false)
    private Long clipId;

    // Snapshot at contest end — preserved even if submitter later changes their display name
    @Column(nullable = false)
    private String submitterName;

    @Column(nullable = false)
    private int voteCount;

    @Column(nullable = false, updatable = false)
    private Instant recordedAt;

    @PrePersist
    void prePersist() {
        recordedAt = Instant.now();
    }
}
