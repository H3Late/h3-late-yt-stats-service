package com.h3late.stats.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "token_claim")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, updatable = false)
    private Instant claimedAt;

    @PrePersist
    void prePersist() {
        claimedAt = Instant.now();
    }
}
