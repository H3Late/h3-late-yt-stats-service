package com.h3late.stats.repository;

import com.h3late.stats.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    boolean existsByVideoIdAndUserNameIgnoreCase(String videoId, String userName);

    // Check for a pending vote (videoId is null) for a user, which can be used to attribute the vote to a stream later on
    boolean existsByVideoIdIsNullAndUserNameIgnoreCase(String userName);

    // Attribute pending votes (videoId is null) to a stream by setting the videoId for all votes that match the userName and have a null videoId
    @Modifying
    @Query("UPDATE Vote v SET v.videoId = :videoId WHERE v.videoId is NULL")
    int attributePendingVotesToStream(@Param("videoId") String videoId);
 

}