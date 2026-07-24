package com.h3late.stats.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "clip_report",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_clip_report_per_reporter",
        columnNames = {"clip_id", "reporter_token"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClipReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clip_id", nullable = false)
    private Long clipId;

    @Column(name = "reporter_token", nullable = false)
    private String reporterToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant reportedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @PrePersist
    void prePersist() {
        reportedAt = Instant.now();
        if (status == null) status = ReportStatus.PENDING;
    }
}
