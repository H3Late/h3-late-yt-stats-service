package com.h3late.stats.repository;

import com.h3late.stats.entity.Contest;
import com.h3late.stats.entity.ContestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
