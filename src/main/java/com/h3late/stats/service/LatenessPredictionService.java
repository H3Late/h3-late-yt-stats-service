package com.h3late.stats.service;

import com.h3late.stats.dto.LatenessPredictionRequest;
import com.h3late.stats.entity.LatenessPrediction;
import com.h3late.stats.entity.LatenessPredictionLeaderboardEntry;
import com.h3late.stats.repository.LatenessPredictionLeaderboardRepository;
import com.h3late.stats.repository.LatenessPredictionRepository;
import com.h3late.stats.security.IdentityResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LatenessPredictionService {
    private final LatenessPredictionRepository predictionRepository;
    private final LatenessPredictionLeaderboardRepository leaderboardRepository;

    public LatenessPredictionService(LatenessPredictionRepository predictionRepository,
                                     LatenessPredictionLeaderboardRepository leaderboardRepository) {
        this.predictionRepository = predictionRepository;
        this.leaderboardRepository = leaderboardRepository;
    }

    public LatenessPrediction submitPrediction(LatenessPredictionRequest request, String identity) {

        validatePredictionRequest(request, identity);

        LatenessPrediction prediction = LatenessPrediction.builder()
            .userId(request.getUserId())
            .userName(request.getUserName().trim())
            .diffSeconds(request.getDiffSeconds())
            .build();

        return predictionRepository.save(prediction);
    }

    public Page<LatenessPredictionLeaderboardEntry> getLatestLeaderboard(String search, Pageable pageable) {
        return leaderboardRepository.searchLeaderboard(search, pageable);
    }

    private void validatePredictionRequest(LatenessPredictionRequest request, String identity) {
        if (request.getUserName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty!");
        }

        if (request.getDiffSeconds() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "diffSeconds must be a non-negative integer!");
        }
        if (predictionRepository.existsByVideoIdIsNullAndUserIdIgnoreCase(identity)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User has already voted!");
        }

    }

    @Transactional
    public void attributePendingPredictions(String videoId) {
        predictionRepository.attributePendingPredictionsToStream(videoId);
    }
}
