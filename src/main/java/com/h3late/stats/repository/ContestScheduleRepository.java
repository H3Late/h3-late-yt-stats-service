package com.h3late.stats.repository;

import com.h3late.stats.entity.ContestSchedule;
import com.h3late.stats.entity.ScheduleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ContestScheduleRepository extends JpaRepository<ContestSchedule, Long> {
    List<ContestSchedule> findByStatusAndScheduledStartAtLessThanEqual(ScheduleStatus status, Instant now);
    Page<ContestSchedule> findAllByOrderByScheduledStartAtDesc(Pageable pageable);
}
