package com.h3late.stats.service;

import com.h3late.stats.entity.LeaderboardEntry;
import com.h3late.stats.entity.Livestream;
import com.h3late.stats.entity.StreamStatus;
import com.h3late.stats.entity.Vote;
import com.h3late.stats.repository.LeaderboardRepository;
import com.h3late.stats.repository.LivestreamRepository;
import com.h3late.stats.repository.VoteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class VoteService {
    private final VoteRepository voteRepository;
    private final LivestreamRepository livestreamRepository;
    private final LeaderboardRepository leaderboardRepository;

    public VoteService(VoteRepository voteRepository, LivestreamRepository livestreamRepository, LeaderboardRepository leaderboardRepository) {
        this.voteRepository = voteRepository;
        this.livestreamRepository = livestreamRepository;
        this.leaderboardRepository = leaderboardRepository;
    }
    public Vote castVote(String videoId, Vote voteRequest) {
        // 1. Find the livestream
        Livestream stream = livestreamRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stream not found"));

        // 2. Logic: Only allow voting if status is SCHEDULED
        if (stream.getStatus() != StreamStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voting is only open for scheduled streams!");
        }

        // 3. Logic: Check for duplicate name for this video
        if (voteRepository.existsByVideoIdAndUserNameIgnoreCase(videoId, voteRequest.getUserName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That name has already voted for this stream!");
        }

        voteRequest.setVideoId(videoId);
        return voteRepository.save(voteRequest);
    }

    public Page<LeaderboardEntry> getLatestLeaderboard(String search, Pageable pageable) {
        return leaderboardRepository.searchLeaderboard(search, pageable);
    }




}