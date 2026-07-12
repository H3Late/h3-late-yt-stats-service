package com.h3late.stats.service;

import com.h3late.stats.entity.LatenessPrediction;
import com.h3late.stats.entity.LatenessPredictionLeaderboardEntry;
import com.h3late.stats.repository.LatenessPredictionLeaderboardRepository;
import com.h3late.stats.repository.LatenessPredictionRepository;
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

    public LatenessPrediction submitPrediction(LatenessPrediction request) {
        String username = request.getUserName();

        if (username == null || username.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty!");
        }

        if (request.getDiffSeconds() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "diffSeconds must be a non-negative integer!");
        }

        username = username.trim();

        if (predictionRepository.existsByVideoIdIsNullAndUserNameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That name has already voted!");
        }

        request.setUserName(username);
        request.setVideoId(null);
        return predictionRepository.save(request);
    }

    public Page<LatenessPredictionLeaderboardEntry> getLatestLeaderboard(String search, Pageable pageable) {
        return leaderboardRepository.searchLeaderboard(search, pageable);
    }

    @Transactional
    public void attributePendingPredictions(String videoId) {
        predictionRepository.attributePendingPredictionsToStream(videoId);
    }
}
