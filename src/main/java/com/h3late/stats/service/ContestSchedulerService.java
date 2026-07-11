package com.h3late.stats.service;

import com.h3late.stats.entity.*;
import com.h3late.stats.repository.ContestClipRepository;
import com.h3late.stats.repository.ContestRepository;
import com.h3late.stats.repository.ContestResultRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
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

    // TODO: consolidate these contest.* @Value fields into a @ConfigurationProperties(prefix = "contest")
    // class — would fix the IDE "unknown property" warnings on application.yaml (no metadata exists for
    // ad-hoc @Value keys) and give type-safe/validated binding. Separate cleanup, not part of this PR.
    @Value("${contest.rotation-cron:0 0 0 * * SUN}")
    private String contestRotationCron;

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
    private final JdbcTemplate jdbcTemplate;

    /**
     * Fails fast at startup if contest.rotation-cron is not a valid Spring cron expression,
     * instead of only surfacing the error later inside an ApplicationReadyEvent listener or a
     * scheduled tick.
     */
    @PostConstruct
    void validateRotationCron() {
        try {
            CronExpression.parse(contestRotationCron);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                "Invalid contest.rotation-cron value: '" + contestRotationCron + "'", e);
        }
    }

    /**
     * Enforces "at most one ACTIVE contest" at the database level, so it holds even across
     * multiple server instances (Hibernate's ddl-auto=update doesn't manage this — there's no
     * migration tool in this project, so it's created here instead). startNewContest() relies
     * on the resulting constraint violation to detect and back off from a losing race.
     */
    @PostConstruct
    void ensureSingleActiveContestConstraint() {
        jdbcTemplate.execute(
            "CREATE UNIQUE INDEX IF NOT EXISTS ux_contest_single_active ON contest (status) WHERE status = 'ACTIVE'"
        );
    }

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
    @Scheduled(cron = "${contest.rotation-cron:0 0 0 * * SUN}", zone = "UTC")
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
        // Atomically claims the ACTIVE -> ENDED transition. If another instance already ended
        // this contest, this returns 0 and we skip recomputing/re-recording the winners.
        int updated = contestRepo.endIfActive(contest.getId());
        if (updated == 0) {
            log.info("Contest id={} already ended by another instance — skipping", contest.getId());
            return;
        }

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

        try {
            contest = contestRepo.save(contest);
            log.info("Started new contest id={}, ends at {}", contest.getId(), scheduledEnd);
        } catch (DataIntegrityViolationException e) {
            log.info("Another instance already started the next active contest — skipping");
        }
    }

    private Instant computeNextCronTick(Instant from) {
        CronExpression expr = CronExpression.parse(contestRotationCron);
        LocalDateTime fromLocal = LocalDateTime.ofInstant(from, ZoneOffset.UTC);
        LocalDateTime next = expr.next(fromLocal);
        if (next == null) {
            throw new IllegalStateException("Cron expression yielded no next occurrence: " + contestRotationCron);
        }
        return next.toInstant(ZoneOffset.UTC);
    }
}
