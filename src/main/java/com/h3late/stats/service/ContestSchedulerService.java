package com.h3late.stats.service;

import com.h3late.stats.dto.ContestScheduleRequest;
import com.h3late.stats.entity.*;
import com.h3late.stats.repository.ContestRepository;
import com.h3late.stats.repository.ContestScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContestSchedulerService {

    private final ContestScheduleRepository scheduleRepo;
    private final ContestRepository contestRepo;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("Running contest schedule check on startup");
        processPendingSchedules();
    }

    @Scheduled(fixedDelay = 60_000)
    public void checkSchedules() {
        processPendingSchedules();
    }

    private synchronized void processPendingSchedules() {
        List<ContestSchedule> pending = scheduleRepo.findByStatusAndScheduledStartAtLessThanEqual(
            ScheduleStatus.PENDING, Instant.now()
        );

        for (ContestSchedule schedule : pending) {
            try {
                Contest contest = Contest.builder()
                    .type(schedule.getType())
                    .startDate(schedule.getScheduledStartAt())
                    .endDate(schedule.getScheduledStartAt().plus(schedule.getDurationDays(), ChronoUnit.DAYS))
                    .status(ContestStatus.ACTIVE)
                    .dailyVoteBudget(schedule.getDailyVoteBudget())
                    .maxClipDurationSeconds(schedule.getMaxClipDurationSeconds())
                    .maxSubmissionsPerUser(schedule.getMaxSubmissionsPerUser())
                    .voteRefreshSchedule(schedule.getVoteRefreshSchedule())
                    .build();

                contest = contestRepo.save(contest);

                schedule.setStatus(ScheduleStatus.PROCESSED);
                schedule.setContestId(contest.getId());
                scheduleRepo.save(schedule);

                log.info("Started contest id={} from schedule id={}", contest.getId(), schedule.getId());
            } catch (Exception e) {
                log.error("Failed to start contest from schedule id={}", schedule.getId(), e);
                schedule.setStatus(ScheduleStatus.FAILED);
                schedule.setFailureReason(e.getMessage());
                scheduleRepo.save(schedule);
            }
        }
    }

    public ContestSchedule createSchedule(ContestScheduleRequest req) {
        if (req.getType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contest type is required");
        }
        if (req.getScheduledStartAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scheduled start time is required");
        }
        if (req.getDurationDays() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duration must be greater than 0");
        }
        if (req.getDailyVoteBudget() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Daily vote budget must be greater than 0");
        }
        if (req.getMaxClipDurationSeconds() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max clip duration must be greater than 0");
        }
        if (req.getMaxSubmissionsPerUser() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max submissions per user must be greater than 0");
        }
        if (req.getVoteRefreshSchedule() == null || req.getVoteRefreshSchedule().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vote refresh schedule is required (e.g. DAILY or MON,THU)");
        }

        ContestSchedule schedule = ContestSchedule.builder()
            .type(req.getType())
            .scheduledStartAt(req.getScheduledStartAt())
            .durationDays(req.getDurationDays())
            .dailyVoteBudget(req.getDailyVoteBudget())
            .maxClipDurationSeconds(req.getMaxClipDurationSeconds())
            .maxSubmissionsPerUser(req.getMaxSubmissionsPerUser())
            .voteRefreshSchedule(req.getVoteRefreshSchedule())
            .status(ScheduleStatus.PENDING)
            .build();

        return scheduleRepo.save(schedule);
    }

    public Page<ContestSchedule> listSchedules(Pageable pageable) {
        return scheduleRepo.findAllByOrderByScheduledStartAtDesc(pageable);
    }
}
