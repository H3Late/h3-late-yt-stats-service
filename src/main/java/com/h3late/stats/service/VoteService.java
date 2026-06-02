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
import org.springframework.transaction.annotation.Transactional;

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

    // Main method for casting votes, 
    // which is used for pre-voting before the stream starts.
    // It checks for duplicate names across all pending votes (videoId is null) 
    // and then saves the vote with a null videoId, allowing it to be attributed to a stream later on.
    public Vote castVote(Vote voteRequest) {
        // For pending votes (without videoId), 
        // we only check for duplicate names across all pending votes, 
        // allowing the same name to be used in different streams as long as they are not pending at the same time.
        String username = voteRequest.getUserName(); 

        if(username == null || username.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty!");
        }

        if(voteRequest.getDiffSeconds() == null || voteRequest.getDiffSeconds() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "diffSeconds must be a non-negative integer!");
        }

        username = username.trim(); 
 
        if (voteRepository.existsByVideoIdIsNullAndUserNameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That name has already voted!");
        }

        voteRequest.setUserName(username); // Trim whitespace from username
        voteRequest.setVideoId(null); // Ensure the videoId is null for pending votes 
        return voteRepository.save(voteRequest);
    }

    public Page<LeaderboardEntry> getLatestLeaderboard(String search, Pageable pageable) {
        return leaderboardRepository.searchLeaderboard(search, pageable);
    }

    // This method can be called after a stream ends to attribute any pending votes to the stream, 
    // allowing them to be counted in the final leaderboard 
    @Transactional 
    public void attributePendingVotes(String videoId) {
        voteRepository.attributePendingVotesToStream(videoId); 
    } 

}