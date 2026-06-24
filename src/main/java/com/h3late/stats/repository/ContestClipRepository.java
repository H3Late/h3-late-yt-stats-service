package com.h3late.stats.repository;

import com.h3late.stats.entity.ContestClip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContestClipRepository extends JpaRepository<ContestClip, Long> {

    Page<ContestClip> findByContestIdAndRemovedFalse(Long contestId, Pageable pageable);

    Optional<ContestClip> findByIdAndRemovedFalse(Long id);

    long countByContestIdAndSubmitterToken(Long contestId, String submitterToken);

    @Query("SELECT cc FROM ContestClip cc WHERE cc.contestId = :contestId AND cc.removed = false ORDER BY cc.voteCount DESC, cc.submittedAt ASC")
    List<ContestClip> findTopWinners(@Param("contestId") Long contestId, Pageable pageable);

    @Modifying
    @Query("UPDATE ContestClip cc SET cc.voteCount = cc.voteCount + 1 WHERE cc.id = :clipId")
    void incrementVoteCount(@Param("clipId") Long clipId);

    @Modifying
    @Query("UPDATE ContestClip cc SET cc.voteCount = cc.voteCount - 1 WHERE cc.id = :clipId AND cc.voteCount > 0")
    void decrementVoteCount(@Param("clipId") Long clipId);
}
