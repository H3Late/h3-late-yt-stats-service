package com.h3late.stats.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

@Entity
@Table(name = "vote")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true) // Set to true in order to not tie the entity to a specific video, allowing for more flexible use cases
    private String videoId; // Foreign key (logical or actual)

    @Column(nullable = false)
    private String userName;

    @Column(nullable = false)
    private Integer diffSeconds;

    private LocalDateTime createdAt = LocalDateTime.now();

}