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
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    public void claim_withBlankToken_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accountClaimService.claim(1L, ""));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    public void claim_newToken_attachesHistoryAndReturnsCounts() {
        when(tokenClaimRepo.findByToken("anon-token")).thenReturn(Optional.empty());
        when(clipRepo.attachUserId("anon-token", 1L)).thenReturn(2);
        when(voteRepo.attachUserId("anon-token", 1L)).thenReturn(5);
        when(reportRepo.attachUserId("anon-token", 1L)).thenReturn(0);

        AccountClaimService.ClaimResult result = accountClaimService.claim(1L, "anon-token");

        assertFalse(result.alreadyClaimed());
        assertEquals(2, result.clipsClaimed());
        assertEquals(5, result.votesClaimed());
        assertEquals(0, result.reportsClaimed());
        verify(tokenClaimRepo).save(any(TokenClaim.class));
    }

    @Test
    public void claim_calledTwiceBySameUser_isIdempotent() {
        TokenClaim existing = TokenClaim.builder().token("anon-token").userId(1L).build();
        when(tokenClaimRepo.findByToken("anon-token")).thenReturn(Optional.of(existing));

        AccountClaimService.ClaimResult result = accountClaimService.claim(1L, "anon-token");

        assertTrue(result.alreadyClaimed());
        verify(tokenClaimRepo, never()).save(any());
        verifyNoInteractions(clipRepo, voteRepo, reportRepo);
    }

    @Test
    public void claim_tokenOwnedByDifferentUser_throwsConflict() {
        TokenClaim existing = TokenClaim.builder().token("anon-token").userId(999L).build();
        when(tokenClaimRepo.findByToken("anon-token")).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accountClaimService.claim(1L, "anon-token"));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    public void claim_concurrentClaimBySameUser_isTreatedAsIdempotent() {
        when(tokenClaimRepo.findByToken("anon-token"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(TokenClaim.builder().token("anon-token").userId(1L).build()));
        when(tokenClaimRepo.save(any(TokenClaim.class)))
                .thenThrow(new DataIntegrityViolationException("concurrent claim"));

        AccountClaimService.ClaimResult result = accountClaimService.claim(1L, "anon-token");

        assertTrue(result.alreadyClaimed());
        verifyNoInteractions(clipRepo, voteRepo, reportRepo);
    }

    @Test
    public void claim_concurrentClaimByDifferentUser_throwsConflict() {
        when(tokenClaimRepo.findByToken("anon-token"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(TokenClaim.builder().token("anon-token").userId(999L).build()));
        when(tokenClaimRepo.save(any(TokenClaim.class)))
                .thenThrow(new DataIntegrityViolationException("concurrent claim"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accountClaimService.claim(1L, "anon-token"));
        assertEquals(409, ex.getStatusCode().value());
    }
}
