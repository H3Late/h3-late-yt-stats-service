package com.h3late.stats.service;

import com.h3late.stats.entity.TokenClaim;
import com.h3late.stats.repository.ClipReportRepository;
import com.h3late.stats.repository.ClipVoteRepository;
import com.h3late.stats.repository.ContestClipRepository;
import com.h3late.stats.repository.TokenClaimRepository;
import com.h3late.stats.security.AccountIdentity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Claiming now happens automatically, server-side, at the moment a new account is created
 * (see SecurityConfig's post-login success handler) — never client-triggered, never on a
 * returning login. Best-effort: a failure here must never block account creation/login.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountClaimService {

    private final TokenClaimRepository tokenClaimRepo;
    private final ContestClipRepository clipRepo;
    private final ClipVoteRepository voteRepo;
    private final ClipReportRepository reportRepo;

    @Transactional
    public ClaimResult claimIfEligible(Long userId, String anonymousToken) {
        if (anonymousToken == null || anonymousToken.isBlank()) {
            return ClaimResult.none();
        }

        try {
            if (tokenClaimRepo.existsByUserId(userId)) {
                return ClaimResult.none();
            }

            Optional<TokenClaim> existing = tokenClaimRepo.findByToken(anonymousToken);
            if (existing.isPresent()) {
                if (!existing.get().getUserId().equals(userId)) {
                    log.warn("Skipping claim: token already linked to a different account (userId={})", userId);
                }
                return ClaimResult.none();
            }

            try {
                tokenClaimRepo.save(TokenClaim.builder().token(anonymousToken).userId(userId).build());
            } catch (DataIntegrityViolationException e) {
                log.warn("Skipping claim: lost a race on token claim (userId={})", userId);
                return ClaimResult.none();
            }

            String newIdentity = AccountIdentity.of(userId);
            int clips = clipRepo.reassignUserId(anonymousToken, newIdentity);
            int votes = voteRepo.reassignUserId(anonymousToken, newIdentity);
            int reports = reportRepo.reassignUserId(anonymousToken, newIdentity);
            return new ClaimResult(clips, votes, reports);
        } catch (Exception e) {
            log.warn("Best-effort claim failed for userId={}", userId, e);
            return ClaimResult.none();
        }
    }

    public record ClaimResult(int clipsClaimed, int votesClaimed, int reportsClaimed) {
        public static ClaimResult none() {
            return new ClaimResult(0, 0, 0);
        }

        public boolean claimedAnything() {
            return clipsClaimed > 0 || votesClaimed > 0 || reportsClaimed > 0;
        }
    }
}
