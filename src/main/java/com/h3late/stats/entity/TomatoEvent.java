package com.h3late.stats.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "tomato_event",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_tomato_event_message",
                columnNames = "message_id"
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TomatoEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private String eventId;

    @Column(name = "video_id", nullable = false)
    private String videoId;

    @Column(name = "live_chat_id", nullable = false)
    private String liveChatId;

    @Column(name = "message_id", nullable = false)
    private String messageId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "user_display_name")
    private String userDisplayName;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "tomato_count", nullable = false)
    private int tomatoCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}