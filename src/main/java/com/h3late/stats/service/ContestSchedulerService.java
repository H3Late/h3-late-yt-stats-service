package com.h3late.stats.service;

import com.h3late.stats.entity.*;
import com.h3late.stats.repository.ContestClipRepository;
import com.h3late.stats.repository.ContestRepository;
import com.h3late.stats.repository.ContestResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContestSchedulerService {

    @Value("${contest.cron:0 0 0 * * SUN}")
    private String cronExpression;

    @Value("${contest.type:WEEKLY}")
    private ContestType contestType;

    @Value("${contest.daily-vote-budget:5}")
    private int dailyVoteBudget;

    @Value("${contest.max-clip-duration-seconds:180}")
    private int maxClipDurationSeconds;

    @Value("${contest.max-submissions-per-user:3}")
    private int maxSubmissionsPerUser;

    @Value("${contest.vote-refresh-schedule:WEEKLY}")
    private String voteRefreshSchedule;

    private final ContestRepository contestRepo;
    private final ContestClipRepository clipRepo;
    private final ContestResultRepository resultRepo;

    /**
     * On startup:
     * 1. End any active contest whose scheduled end time has already passed.
     * 2. If no active contest exists after step 1, create one ending at the next cron tick.
     */
    @EventListener(ApplicationReadyEvent.class)
    public synchronized void onStartup() {
        log.info("Contest startup check");
        endElapsedContests();
        ensureActiveContest();
    }

    /**
     * On each cron tick: end all active contests, then start a new one ending at the tick after this one.
     */
    @Scheduled(cron = "${contest.cron:0 0 0 * * SUN}", zone = "UTC")
    public synchronized void onCronTick() {
        log.info("Contest cron tick — rotating contest");
        endAllActiveContests();
        startNewContest();
    }

    // --- private ---

    private void endElapsedContests() {
        List<Contest> elapsed = contestRepo.findByStatusAndEndDateLessThanEqual(ContestStatus.ACTIVE, Instant.now());
        elapsed.forEach(this::endContest);
    }

    private void endAllActiveContests() {
        List<Contest> active = contestRepo.findAllByStatus(ContestStatus.ACTIVE);
        active.forEach(this::endContest);
    }

    @Transactional
    private void endContest(Contest contest) {
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

    private void ensureActiveContest() {
        boolean hasActive = contestRepo.findFirstByStatus(ContestStatus.ACTIVE).isPresent();
        if (!hasActive) {
            log.info("No active contest found — starting one");
            startNewContest();
        }
    }

    private void startNewContest() {
        Instant now = Instant.now();
        Instant scheduledEnd = computeNextCronTick(now);

        Contest contest = Contest.builder()
            .type(contestType)
            .startDate(now)
            .endDate(scheduledEnd)
            .status(ContestStatus.ACTIVE)
            .dailyVoteBudget(dailyVoteBudget)
            .maxClipDurationSeconds(maxClipDurationSeconds)
            .maxSubmissionsPerUser(maxSubmissionsPerUser)
            .voteRefreshSchedule(voteRefreshSchedule)
            .build();

        contest = contestRepo.save(contest);
        log.info("Started new contest id={}, ends at {}", contest.getId(), scheduledEnd);
    }

    private Instant computeNextCronTick(Instant from) {
        CronExpression expr = CronExpression.parse(cronExpression);
        LocalDateTime fromLocal = LocalDateTime.ofInstant(from, ZoneOffset.UTC);
        LocalDateTime next = expr.next(fromLocal);
        if (next == null) {
            throw new IllegalStateException("Cron expression yielded no next occurrence: " + cronExpression);
        }
        return next.toInstant(ZoneOffset.UTC);
    }
}
