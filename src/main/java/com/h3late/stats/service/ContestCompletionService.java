package com.h3late.stats.service;

import com.h3late.stats.entity.*;
import com.h3late.stats.repository.ContestClipRepository;
import com.h3late.stats.repository.ContestRepository;
import com.h3late.stats.repository.ContestResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContestCompletionService {

    private final ContestRepository contestRepo;
    private final ContestClipRepository clipRepo;
    private final ContestResultRepository resultRepo;

    @Scheduled(fixedDelay = 60_000)
    public void checkForEndedContests() {
        List<Contest> expired = contestRepo.findByStatusAndEndDateLessThanEqual(
            ContestStatus.ACTIVE, Instant.now()
        );
        for (Contest contest : expired) {
            endContest(contest);
        }
    }

    @Transactional
    public void endContest(Contest contest) {
        if (contest.getStatus() != ContestStatus.ACTIVE) return;

        List<ContestClip> topClips = clipRepo.findTopWinners(contest.getId(), PageRequest.of(0, 3));

        for (int i = 0; i < topClips.size(); i++) {
            ContestClip clip = topClips.get(i);
            resultRepo.save(ContestResult.builder()
                .contestId(contest.getId())
                .rank(i + 1)
                .clipId(clip.getId())
                .submitterName(clip.getSubmitterName())
                .voteCount(clip.getVoteCount())
                .build());
        }

        contest.setStatus(ContestStatus.ENDED);
        contestRepo.save(contest);

        log.info("Contest id={} ended with {} winner(s)", contest.getId(), topClips.size());
    }
}
