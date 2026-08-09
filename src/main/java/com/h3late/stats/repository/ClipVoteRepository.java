package com.h3late.stats.repository;

import com.h3late.stats.entity.ClipVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClipVoteRepository extends JpaRepository<ClipVote, Long> {

    // Budget check: how many votes has this user cast in the current period for this contest
    long countByUserIdAndContestIdAndVotePeriodStart(String userId, Long contestId, Instant votePeriodStart);

    // Voter status: which clips did this user vote for in the current period
    List<ClipVote> findByUserIdAndContestIdAndVotePeriodStart(String userId, Long contestId, Instant votePeriodStart);

    // Un-vote: find the specific vote to delete
    Optional<ClipVote> findByClipIdAndUserIdAndVotePeriodStart(Long clipId, String userId, Instant votePeriodStart);

    @Modifying
    @Query("UPDATE ClipVote cv SET cv.userId = :newIdentity WHERE cv.userId = :oldToken")
    int reassignUserId(@Param("oldToken") String oldToken, @Param("newIdentity") String newIdentity);
}
