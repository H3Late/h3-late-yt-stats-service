package com.h3late.stats.repository;

import com.h3late.stats.entity.LatenessPredictionLeaderboardEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LatenessPredictionLeaderboardRepository extends JpaRepository<LatenessPredictionLeaderboardEntry, String> {
    @Query("SELECT e FROM LatenessPredictionLeaderboardEntry e WHERE " +
            "(:search IS NULL OR LOWER(e.userName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<LatenessPredictionLeaderboardEntry> searchLeaderboard(
            @Param("search") String search,
            Pageable pageable);
}
