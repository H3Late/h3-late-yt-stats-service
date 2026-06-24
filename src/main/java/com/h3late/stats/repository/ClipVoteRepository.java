package com.h3late.stats.repository;

import com.h3late.stats.entity.ClipVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClipVoteRepository extends JpaRepository<ClipVote, Long> {

    // Budget check: how many votes has this voter cast in the current period for this contest
    long countByVoterTokenAndContestIdAndVotePeriodStart(String voterToken, Long contestId, Instant votePeriodStart);

    // Voter status: which clips did this voter vote for in the current period
    List<ClipVote> findByVoterTokenAndContestIdAndVotePeriodStart(String voterToken, Long contestId, Instant votePeriodStart);

    // Un-vote: find the specific vote to delete
    Optional<ClipVote> findByClipIdAndVoterTokenAndVotePeriodStart(Long clipId, String voterToken, Instant votePeriodStart);
}
