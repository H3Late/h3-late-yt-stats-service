package com.h3late.stats.repository;

import com.h3late.stats.entity.LatenessPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

public interface LatenessPredictionRepository extends JpaRepository<LatenessPrediction, Long> {
    boolean existsByVideoIdAndUserNameIgnoreCase(String videoId, String userName);

    boolean existsByVideoIdIsNullAndUserNameIgnoreCase(String userName);

    boolean existsByVideoIdIsNullAndUserIdIgnoreCase(String userId);

    @Modifying
    @Query("UPDATE LatenessPrediction v SET v.videoId = :videoId WHERE v.videoId is NULL")
    int attributePendingPredictionsToStream(@Param("videoId") String videoId);
}
