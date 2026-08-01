package com.h3late.stats.repository;

import com.h3late.stats.entity.ClipReport;
import com.h3late.stats.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClipReportRepository extends JpaRepository<ClipReport, Long> {
    Page<ClipReport> findByStatus(ReportStatus status, Pageable pageable);
    boolean existsByClipIdAndReporterToken(Long clipId, String reporterToken);

    @Modifying
    @Query("UPDATE ClipReport cr SET cr.userId = :userId WHERE cr.reporterToken = :token AND cr.userId IS NULL")
    int attachUserId(@Param("token") String token, @Param("userId") Long userId);
}
