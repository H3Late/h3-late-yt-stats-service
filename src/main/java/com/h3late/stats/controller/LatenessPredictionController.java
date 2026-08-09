package com.h3late.stats.controller;

import com.h3late.stats.dto.LatenessPredictionRequest;
import com.h3late.stats.entity.LatenessPrediction;
import com.h3late.stats.entity.LatenessPredictionLeaderboardEntry;
import com.h3late.stats.security.IdentityResolver;
import com.h3late.stats.service.LatenessPredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vote")
@RequiredArgsConstructor()
public class LatenessPredictionController {

    private final LatenessPredictionService predictionService;
    private final IdentityResolver identityResolver;

    @PostMapping
    public ResponseEntity<LatenessPrediction> submitPrediction(
            @RequestBody LatenessPredictionRequest request,
            Authentication authentication
    ) {
        String identity = identityResolver.resolve(authentication, request.getUserToken());
        return ResponseEntity.ok(predictionService.submitPrediction(request, identity));
    }

    @GetMapping("/leaderboard/latest")
    public ResponseEntity<Page<LatenessPredictionLeaderboardEntry>> getLatestLeaderboard(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "proximityScore", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ResponseEntity.ok(predictionService.getLatestLeaderboard(search, pageable));
    }
}
