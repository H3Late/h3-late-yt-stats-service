package com.h3late.stats.repository;

import com.h3late.stats.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    boolean existsByVideoIdAndUserNameIgnoreCase(String videoId, String userName);
}