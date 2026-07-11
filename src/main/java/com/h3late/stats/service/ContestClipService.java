package com.h3late.stats.service;

import com.h3late.stats.dto.ClipReportRequest;
import com.h3late.stats.dto.ClipSubmissionRequest;
import com.h3late.stats.dto.VoterStatusResponse;
import com.h3late.stats.entity.*;
import com.h3late.stats.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContestClipService {

    @Value("${contest.min-clip-duration-seconds:5}")
    private int minClipDurationSeconds;

    private final ContestRepository contestRepo;
    private final ContestClipRepository clipRepo;
    private final ClipVoteRepository voteRepo;
    private final ClipReportRepository reportRepo;
    private final ContestResultRepository resultRepo;
    private final LivestreamRepository livestreamRepo;
    private final VotePeriodService votePeriodService;

    // --- Contest queries ---

    public Optional<Contest> findActiveContest() {
        return contestRepo.findFirstByStatus(ContestStatus.ACTIVE);
    }

    @Transactional
    public Contest patchActiveContest(Integer maxClipDurationSeconds, Integer maxSubmissionsPerUser, Integer dailyVoteBudget) {
        Contest contest = contestRepo.findFirstByStatus(ContestStatus.ACTIVE)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active contest"));
        if (maxClipDurationSeconds != null) contest.setMaxClipDurationSeconds(maxClipDurationSeconds);
        if (maxSubmissionsPerUser != null) contest.setMaxSubmissionsPerUser(maxSubmissionsPerUser);
        if (dailyVoteBudget != null) contest.setDailyVoteBudget(dailyVoteBudget);
        return contestRepo.save(contest);
    }

    public Contest getContest(Long id) {
        return contestRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contest not found"));
    }

    public Page<Contest> getContestHistory(Pageable pageable) {
        return contestRepo.findByStatusOrderByEndDateDesc(ContestStatus.ENDED, pageable);
    }

    public List<ContestResult> getContestResults(Long id) {
        Contest contest = getContest(id);
        if (contest.getStatus() != ContestStatus.ENDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contest has not ended yet");
        }
        return resultRepo.findByContestIdOrderByRankAsc(id);
    }

    // --- Eligible streams ---

    public List<Livestream> getEligibleStreams(Long contestId) {
        Contest contest = getContest(contestId);
        return livestreamRepo.findEligibleStreams(StreamStatus.ENDED, contest.getStartDate(), contest.getEndDate());
    }

    // --- Clips ---

    @Transactional
    public ContestClip submitClip(Long contestId, ClipSubmissionRequest req) {
        Contest contest = getActiveContestById(contestId);
        validateSubmissionRequest(req);

        Livestream stream = livestreamRepo.findById(req.getVideoId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Livestream not found"));

        if (stream.getStatus() != StreamStatus.ENDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stream must have ENDED status to be clipped");
        }

        if (stream.getActualStart() == null
                || stream.getActualStart().isBefore(contest.getStartDate())
                || stream.getActualStart().isAfter(contest.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stream did not start during this contest period");
        }

        int clipDuration = getClipDuration(req);
        if (clipDuration > contest.getMaxClipDurationSeconds()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Clip exceeds maximum duration of " + contest.getMaxClipDurationSeconds() + " seconds");
        }

        if (clipDuration < minClipDurationSeconds) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Clip must be at least " + minClipDurationSeconds + " seconds long");
        }

        if (stream.getTotalDurationSeconds() != null && req.getEndSeconds() > stream.getTotalDurationSeconds()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End timestamp exceeds stream duration");
        }

        long submissionCount = clipRepo.countByContestIdAndSubmitterTokenAndRemovedFalse(contestId, req.getSubmitterToken());
        if (submissionCount >= contest.getMaxSubmissionsPerUser()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Submission limit of " + contest.getMaxSubmissionsPerUser() + " clips per contest reached");
        }

        ContestClip clip = ContestClip.builder()
            .contestId(contestId)
            .videoId(req.getVideoId())
            .title(req.getTitle().trim())
            .description(req.getDescription() != null ? req.getDescription().trim() : null)
            .startSeconds(req.getStartSeconds())
            .endSeconds(req.getEndSeconds())
            .submitterToken(req.getSubmitterToken())
            .submitterName(req.getSubmitterName().trim())
            .build();

        return clipRepo.save(clip);
    }

    private int getClipDuration(ClipSubmissionRequest req) {
        int clipDuration = req.getEndSeconds() - req.getStartSeconds();

        if (req.getStartSeconds() < 0 || req.getEndSeconds() <= req.getStartSeconds()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid timestamp range: start must be >= 0 and end must be after start");
        }
        return clipDuration;
    }

    public Page<ContestClip> getClips(Long contestId, Pageable pageable) {
        getContest(contestId);
        return clipRepo.findByContestIdAndRemovedFalse(contestId, pageable);
    }

    public ContestClip getClip(Long clipId) {
        return clipRepo.findByIdAndRemovedFalse(clipId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clip not found"));
    }

    // --- Voting ---

    @Transactional
    public void castVote(Long clipId, String voterToken) {
        if (voterToken == null || voterToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voter token is required");
        }

        ContestClip clip = clipRepo.findByIdAndRemovedFalse(clipId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clip not found"));

        Contest contest = contestRepo.findById(clip.getContestId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contest not found"));

        if (contest.getStatus() != ContestStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contest is not active");
        }

        Instant periodStart = votePeriodService.getCurrentPeriodStart(contest.getVoteRefreshSchedule());

        long votesUsed = voteRepo.countByVoterTokenAndContestIdAndVotePeriodStart(voterToken, contest.getId(), periodStart);
        if (votesUsed >= contest.getDailyVoteBudget()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Vote budget of " + contest.getDailyVoteBudget() + " exhausted for this period");
        }

        try {
            voteRepo.save(ClipVote.builder()
                .clipId(clipId)
                .contestId(contest.getId())
                .voterToken(voterToken)
                .votePeriodStart(periodStart)
                .build());

            clipRepo.incrementVoteCount(clipId);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already voted for this clip in the current period");
        }
    }

    @Transactional
    public void retractVote(Long clipId, String voterToken) {
        if (voterToken == null || voterToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voter token is required");
        }

        ContestClip clip = clipRepo.findByIdAndRemovedFalse(clipId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clip not found"));

        Contest contest = contestRepo.findById(clip.getContestId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contest not found"));

        if (contest.getStatus() != ContestStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contest is not active");
        }

        Instant periodStart = votePeriodService.getCurrentPeriodStart(contest.getVoteRefreshSchedule());

        ClipVote vote = voteRepo.findByClipIdAndVoterTokenAndVotePeriodStart(clipId, voterToken, periodStart)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No vote found for this clip in the current period"));

        voteRepo.delete(vote);
        clipRepo.decrementVoteCount(clipId);
    }

    public VoterStatusResponse getVoterStatus(Long contestId, String voterToken) {
        if (voterToken == null || voterToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voter token is required");
        }

        Contest contest = getContest(contestId);

        Instant periodStart = votePeriodService.getCurrentPeriodStart(contest.getVoteRefreshSchedule());
        Instant nextPeriodStart = votePeriodService.getNextPeriodStart(contest.getVoteRefreshSchedule());

        long votesUsed = voteRepo.countByVoterTokenAndContestIdAndVotePeriodStart(voterToken, contestId, periodStart);
        int remaining = (int) Math.max(0, contest.getDailyVoteBudget() - votesUsed);

        List<Long> votedClipIds = voteRepo
            .findByVoterTokenAndContestIdAndVotePeriodStart(voterToken, contestId, periodStart)
            .stream()
            .map(ClipVote::getClipId)
            .toList();

        return new VoterStatusResponse(remaining, nextPeriodStart, votedClipIds);
    }

    // --- Reports ---

    public ClipReport reportClip(Long clipId, ClipReportRequest req) {
        if (req.getReporterToken() == null || req.getReporterToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reporter token is required");
        }
        if (req.getReason() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Report reason is required");
        }

        clipRepo.findByIdAndRemovedFalse(clipId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clip not found"));

        if (reportRepo.existsByClipIdAndReporterToken(clipId, req.getReporterToken())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already reported this clip");
        }

        try {
            return reportRepo.save(ClipReport.builder()
                .clipId(clipId)
                .reporterToken(req.getReporterToken())
                .reason(req.getReason())
                .description(req.getDescription())
                .build());
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already reported this clip");
        }
    }

    // --- Admin ---

    @Transactional
    public ContestClip adminRemoveClip(Long clipId, String reason) {
        ContestClip clip = clipRepo.findById(clipId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clip not found"));

        clip.setRemoved(true);
        clip.setRemovedAt(Instant.now());
        clip.setRemovedReason(reason);
        return clipRepo.save(clip);
    }

    public Page<ClipReport> getReports(ReportStatus status, Pageable pageable) {
        if (status != null) {
            return reportRepo.findByStatus(status, pageable);
        }
        return reportRepo.findAll(pageable);
    }

    @Transactional
    public ClipReport updateReportStatus(Long reportId, ReportStatus newStatus) {
        ClipReport report = reportRepo.findById(reportId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        report.setStatus(newStatus);
        return reportRepo.save(report);
    }

    // --- Helpers ---

    private Contest getActiveContestById(Long contestId) {
        Contest contest = getContest(contestId);
        if (contest.getStatus() != ContestStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contest is not active");
        }
        return contest;
    }

    private void validateSubmissionRequest(ClipSubmissionRequest req) {
        if (req.getSubmitterToken() == null || req.getSubmitterToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Submitter token is required");
        }
        if (req.getSubmitterName() == null || req.getSubmitterName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Submitter name is required");
        }
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }
        if (req.getVideoId() == null || req.getVideoId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Video ID is required");
        }
    }
}
