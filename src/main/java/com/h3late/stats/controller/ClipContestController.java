package com.h3late.stats.controller;

import com.h3late.stats.dto.*;
import com.h3late.stats.entity.*;
import com.h3late.stats.service.AdminKeyValidator;
import com.h3late.stats.service.ContestClipService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contest/clip-contest")
@RequiredArgsConstructor
public class ClipContestController {

    private final ContestClipService contestClipService;
    private final AdminKeyValidator adminKeyValidator;

    // -------------------------------------------------------------------------
    // Contest lifecycle
    // -------------------------------------------------------------------------

    @GetMapping("/active")
    public ResponseEntity<Contest> getActiveContest() {
        return contestClipService.findActiveContest()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/history")
    public Page<Contest> getHistory(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return contestClipService.getContestHistory(PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public Contest getContest(@PathVariable Long id) {
        return contestClipService.getContest(id);
    }

    @GetMapping("/{id}/results")
    public List<ContestResult> getResults(@PathVariable Long id) {
        return contestClipService.getContestResults(id);
    }

    // -------------------------------------------------------------------------
    // Eligible streams (for submission form dropdown)
    // -------------------------------------------------------------------------

    @GetMapping("/{contestId}/eligible-streams")
    public List<Livestream> getEligibleStreams(@PathVariable Long contestId) {
        return contestClipService.getEligibleStreams(contestId);
    }

    // -------------------------------------------------------------------------
    // Clips
    // -------------------------------------------------------------------------

    @PostMapping("/{contestId}/clips")
    @ResponseStatus(HttpStatus.CREATED)
    public ContestClip submitClip(
        @PathVariable Long contestId,
        @RequestBody ClipSubmissionRequest req
    ) {
        return contestClipService.submitClip(contestId, req);
    }

    @GetMapping("/{contestId}/clips")
    public Page<ContestClip> getClips(
        @PathVariable Long contestId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size,
            Sort.by(Sort.Direction.DESC, "voteCount").and(Sort.by(Sort.Direction.ASC, "submittedAt")));
        return contestClipService.getClips(contestId, pageable);
    }

    @GetMapping("/clips/{clipId}")
    public ContestClip getClip(@PathVariable Long clipId) {
        return contestClipService.getClip(clipId);
    }

    // -------------------------------------------------------------------------
    // Voting
    // -------------------------------------------------------------------------

    @PostMapping("/clips/{clipId}/vote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void castVote(
        @PathVariable Long clipId,
        @RequestBody VoteRequest req
    ) {
        contestClipService.castVote(clipId, req.getVoterToken());
    }

    @DeleteMapping("/clips/{clipId}/vote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retractVote(
        @PathVariable Long clipId,
        @RequestParam String voterToken
    ) {
        contestClipService.retractVote(clipId, voterToken);
    }

    @GetMapping("/{contestId}/voter/{voterToken}")
    public VoterStatusResponse getVoterStatus(
        @PathVariable Long contestId,
        @PathVariable String voterToken
    ) {
        return contestClipService.getVoterStatus(contestId, voterToken);
    }

    // -------------------------------------------------------------------------
    // Reports
    // -------------------------------------------------------------------------

    @PostMapping("/clips/{clipId}/report")
    @ResponseStatus(HttpStatus.CREATED)
    public ClipReport reportClip(
        @PathVariable Long clipId,
        @RequestBody ClipReportRequest req
    ) {
        return contestClipService.reportClip(clipId, req);
    }

    // -------------------------------------------------------------------------
    // Admin — clip removal and report management
    // -------------------------------------------------------------------------

    @PatchMapping("/admin/active")
    public Contest patchActiveContest(
        @RequestParam(required = false) Integer maxClipDurationSeconds,
        @RequestParam(required = false) Integer maxSubmissionsPerUser,
        @RequestParam(required = false) Integer dailyVoteBudget,
        @RequestHeader("X-Admin-Key") String adminKey
    ) {
        adminKeyValidator.validate(adminKey);
        return contestClipService.patchActiveContest(maxClipDurationSeconds, maxSubmissionsPerUser, dailyVoteBudget);
    }

    @DeleteMapping("/admin/clips/{clipId}")
    public ContestClip adminRemoveClip(
        @PathVariable Long clipId,
        @RequestParam(required = false) String reason,
        @RequestHeader("X-Admin-Key") String adminKey
    ) {
        adminKeyValidator.validate(adminKey);
        return contestClipService.adminRemoveClip(clipId, reason);
    }

    @GetMapping("/admin/reports")
    public Page<ClipReport> getReports(
        @RequestParam(required = false) ReportStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestHeader("X-Admin-Key") String adminKey
    ) {
        adminKeyValidator.validate(adminKey);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reportedAt"));
        return contestClipService.getReports(status, pageable);
    }

    @PatchMapping("/admin/reports/{reportId}")
    public ClipReport updateReport(
        @PathVariable Long reportId,
        @RequestBody ReportStatusUpdateRequest req,
        @RequestHeader("X-Admin-Key") String adminKey
    ) {
        adminKeyValidator.validate(adminKey);
        return contestClipService.updateReportStatus(reportId, req.getStatus());
    }
}
