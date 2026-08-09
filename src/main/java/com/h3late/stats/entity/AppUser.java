package com.h3late.stats.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "app_user")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    private String email;

    private String avatarUrl;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant lastLoginAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    /**
     * Derived, not stored: disambiguating a colliding display name only needs *some* value
     * that's unique per account and never changes — the primary key already is exactly that,
     * for free. No generation, no collision handling, no separate column to keep in sync.
     */
    public String getDiscriminator() {
        return String.valueOf(id);
    }
}
