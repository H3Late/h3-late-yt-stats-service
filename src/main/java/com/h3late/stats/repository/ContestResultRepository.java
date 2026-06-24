package com.h3late.stats.repository;

import com.h3late.stats.entity.ContestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContestResultRepository extends JpaRepository<ContestResult, Long> {
    List<ContestResult> findByContestIdOrderByRankAsc(Long contestId);
}
