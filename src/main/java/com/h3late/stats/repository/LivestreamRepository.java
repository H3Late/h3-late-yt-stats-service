
package com.h3late.stats.repository;

import com.h3late.stats.dto.StatsByDayProjection;
import com.h3late.stats.dto.StatsSummaryProjection;
import com.h3late.stats.entity.Livestream;
import com.h3late.stats.entity.StreamStatus;
import com.h3late.stats.entity.TimeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LivestreamRepository extends JpaRepository<Livestream, String>, JpaSpecificationExecutor<Livestream> {
    @Query(value = "SELECT * FROM h3_global_stats", nativeQuery = true)
    StatsSummaryProjection getGlobalStats();

    @Query(value = "SELECT * FROM h3_stats_by_day", nativeQuery = true)
    List<StatsByDayProjection> getStatsByDay();

    List<Livestream> findAllByStatus(StreamStatus status);

    @Query("SELECT l FROM Livestream l WHERE l.status = :status AND l.actualStart >= :startDate AND l.actualStart <= :endDate ORDER BY l.actualStart DESC")
    List<Livestream> findEligibleStreams(@Param("status") StreamStatus status, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT l FROM Livestream l WHERE " +
            "(:status IS NULL OR l.status = :status) AND " +
            "(:timeStatus IS NULL OR l.timeStatus = :timeStatus) AND " +
            "(:search IS NULL OR LOWER(l.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "OR l.videoId LIKE CONCAT('%', CAST(:search AS string), '%'))")
    Page<Livestream> searchAdvanced(
            @Param("status") StreamStatus status,
            @Param("timeStatus") TimeStatus timeStatus,
            @Param("search") String search,
            Pageable pageable);

}
