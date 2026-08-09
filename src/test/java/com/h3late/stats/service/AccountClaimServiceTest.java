package com.h3late.stats.service;

import com.h3late.stats.entity.TokenClaim;
import com.h3late.stats.repository.ClipReportRepository;
import com.h3late.stats.repository.ClipVoteRepository;
import com.h3late.stats.repository.ContestClipRepository;
import com.h3late.stats.repository.TokenClaimRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AccountClaimServiceTest {

    private TokenClaimRepository tokenClaimRepo;
    private ContestClipRepository clipRepo;
    private ClipVoteRepository voteRepo;
    private ClipReportRepository reportRepo;
    private AccountClaimService accountClaimService;

    @BeforeEach
    public void setUp() {
        tokenClaimRepo = Mockito.mock(TokenClaimRepository.class);
        clipRepo = Mockito.mock(ContestClipRepository.class);
        voteRepo = Mockito.mock(ClipVoteRepository.class);
        reportRepo = Mockito.mock(ClipReportRepository.class);
        accountClaimService = new AccountClaimService(tokenClaimRepo, clipRepo, voteRepo, reportRepo);
    }

    @Test
    public void claimIfEligible_withBlankToken_isNoop() {
        AccountClaimService.ClaimResult result = accountClaimService.claimIfEligible(1L, "");

        assertFalse(result.claimedAnything());
        verifyNoInteractions(tokenClaimRepo, clipRepo, voteRepo, reportRepo);
    }

    @Test
    public void claimIfEligible_userAlreadyHasClaim_isNoop() {
        when(tokenClaimRepo.existsByUserId(1L)).thenReturn(true);

        AccountClaimService.ClaimResult result = accountClaimService.claimIfEligible(1L, "anon-token");

        assertFalse(result.claimedAnything());
        verify(tokenClaimRepo, never()).findByToken(any());
        verifyNoInteractions(clipRepo, voteRepo, reportRepo);
    }

    @Test
    public void claimIfEligible_newToken_attachesHistoryAndReturnsCounts() {
        when(tokenClaimRepo.existsByUserId(1L)).thenReturn(false);
        when(tokenClaimRepo.findByToken("anon-token")).thenReturn(Optional.empty());
        when(clipRepo.reassignUserId("anon-token", "u:1")).thenReturn(2);
        when(voteRepo.reassignUserId("anon-token", "u:1")).thenReturn(5);
        when(reportRepo.reassignUserId("anon-token", "u:1")).thenReturn(0);

        AccountClaimService.ClaimResult result = accountClaimService.claimIfEligible(1L, "anon-token");

        assertTrue(result.claimedAnything());
        assertEquals(2, result.clipsClaimed());
        assertEquals(5, result.votesClaimed());
        assertEquals(0, result.reportsClaimed());
        verify(tokenClaimRepo).save(any(TokenClaim.class));
    }

    @Test
    public void claimIfEligible_tokenAlreadyOwnedByDifferentUser_isNoop() {
        when(tokenClaimRepo.existsByUserId(1L)).thenReturn(false);
        TokenClaim existing = TokenClaim.builder().token("anon-token").userId(999L).build();
        when(tokenClaimRepo.findByToken("anon-token")).thenReturn(Optional.of(existing));

        AccountClaimService.ClaimResult result = accountClaimService.claimIfEligible(1L, "anon-token");

        assertFalse(result.claimedAnything());
        verify(tokenClaimRepo, never()).save(any());
        verifyNoInteractions(clipRepo, voteRepo, reportRepo);
    }

    @Test
    public void claimIfEligible_concurrentClaimRace_isNoop() {
        when(tokenClaimRepo.existsByUserId(1L)).thenReturn(false);
        when(tokenClaimRepo.findByToken("anon-token")).thenReturn(Optional.empty());
        when(tokenClaimRepo.save(any(TokenClaim.class)))
                .thenThrow(new DataIntegrityViolationException("concurrent claim"));

        AccountClaimService.ClaimResult result = accountClaimService.claimIfEligible(1L, "anon-token");

        assertFalse(result.claimedAnything());
        verifyNoInteractions(clipRepo, voteRepo, reportRepo);
    }
}
