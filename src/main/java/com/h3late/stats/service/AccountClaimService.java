package com.h3late.stats.service;

import com.h3late.stats.entity.TokenClaim;
import com.h3late.stats.repository.ClipReportRepository;
import com.h3late.stats.repository.ClipVoteRepository;
import com.h3late.stats.repository.ContestClipRepository;
import com.h3late.stats.repository.TokenClaimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountClaimService {

    private final TokenClaimRepository tokenClaimRepo;
    private final ContestClipRepository clipRepo;
    private final ClipVoteRepository voteRepo;
    private final ClipReportRepository reportRepo;

    /**
     * Idempotent: safe to call on every login. Does not rewrite historical userToken/voterToken/
     * reporterToken values, only attaches userId — a known, accepted narrow gap (see plan doc).
     */
    @Transactional
    public ClaimResult claim(Long userId, String anonymousToken) {
        if (anonymousToken == null || anonymousToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "anonymousToken is required");
        }

        Optional<TokenClaim> existing = tokenClaimRepo.findByToken(anonymousToken);
        if (existing.isPresent()) {
            requireOwnedBy(existing.get(), userId);
            return new ClaimResult(true, 0, 0, 0);
        }

        try {
            tokenClaimRepo.save(TokenClaim.builder().token(anonymousToken).userId(userId).build());
        } catch (DataIntegrityViolationException e) {
            // Lost a race against a concurrent claim of the same token.
            TokenClaim winner = tokenClaimRepo.findByToken(anonymousToken).orElseThrow(() -> e);
            requireOwnedBy(winner, userId);
            return new ClaimResult(true, 0, 0, 0);
        }

        int clips = clipRepo.attachUserId(anonymousToken, userId);
        int votes = voteRepo.attachUserId(anonymousToken, userId);
        int reports = reportRepo.attachUserId(anonymousToken, userId);

        return new ClaimResult(false, clips, votes, reports);
    }

    private void requireOwnedBy(TokenClaim claim, Long userId) {
        if (!claim.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This token is already linked to a different account");
        }
    }

    public record ClaimResult(boolean alreadyClaimed, int clipsClaimed, int votesClaimed, int reportsClaimed) {
    }
}
