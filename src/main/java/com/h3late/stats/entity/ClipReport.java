package com.h3late.stats.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "clip_report")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClipReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long clipId;

    @Column(nullable = false)
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
