package com.h3late.stats.controller;

import com.h3late.stats.entity.LatenessPrediction;
import com.h3late.stats.entity.LatenessPredictionLeaderboardEntry;
import com.h3late.stats.service.LatenessPredictionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vote")
public class LatenessPredictionController {

    private final LatenessPredictionService predictionService;

    public LatenessPredictionController(LatenessPredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @PostMapping
    public ResponseEntity<LatenessPrediction> submitPrediction(@RequestBody LatenessPrediction request) {
        return ResponseEntity.ok(predictionService.submitPrediction(request));
    }

    @GetMapping("/leaderboard/latest")
    public ResponseEntity<Page<LatenessPredictionLeaderboardEntry>> getLatestLeaderboard(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "proximityScore", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ResponseEntity.ok(predictionService.getLatestLeaderboard(search, pageable));
    }
}
