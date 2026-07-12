package com.h3late.stats.repository;

import com.h3late.stats.entity.Contest;
import com.h3late.stats.entity.ContestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContestRepository extends JpaRepository<Contest, Long> {
    Optional<Contest> findFirstByStatus(ContestStatus status);
    List<Contest> findAllByStatus(ContestStatus status);
    Page<Contest> findByStatusOrderByEndDateDesc(ContestStatus status, Pageable pageable);
    List<Contest> findByStatusAndEndDateLessThanEqual(ContestStatus status, Instant now);

    // Atomically transitions ACTIVE -> ENDED; returns 0 if another instance already ended it,
    // so the caller knows whether it "won" the race and should compute/record the winners.
    @Modifying
    @Query("UPDATE Contest c SET c.status = com.h3late.stats.entity.ContestStatus.ENDED " +
           "WHERE c.id = :id AND c.status = com.h3late.stats.entity.ContestStatus.ACTIVE")
    int endIfActive(@Param("id") Long id);
}
