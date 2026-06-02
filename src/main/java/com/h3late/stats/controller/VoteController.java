package com.h3late.stats.controller;

import com.h3late.stats.entity.LeaderboardEntry;
import com.h3late.stats.entity.Vote;
import com.h3late.stats.service.VoteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/vote")
public class VoteController {

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @PostMapping("/{videoId}")
    public ResponseEntity<Vote> castVote(
            @PathVariable String videoId,
            @RequestBody Vote voteRequest) {
        return ResponseEntity.ok(voteService.castVote(videoId, voteRequest));
    }


    @GetMapping("/leaderboard/latest")
    public ResponseEntity<Page<LeaderboardEntry>> getLatestLeaderboard(
            @RequestParam(required = false) String search, // Add optional search param
            @PageableDefault(size = 10, sort = "proximityScore", direction = Sort.Direction.ASC)
            Pageable pageable) {

        // Pass the search string to the service
        return ResponseEntity.ok(voteService.getLatestLeaderboard(search, pageable));
    }

}