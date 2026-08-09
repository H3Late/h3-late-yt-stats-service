package com.h3late.stats.controller;

import com.h3late.stats.dto.LatenessPredictionRequest;
import com.h3late.stats.entity.LatenessPrediction;
import com.h3late.stats.entity.LatenessPredictionLeaderboardEntry;
import com.h3late.stats.security.AppPrincipal;
import com.h3late.stats.security.IdentityResolver;
import com.h3late.stats.service.LatenessPredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vote")
@RequiredArgsConstructor()
public class LatenessPredictionController {

    // Matches the guest-name shape stats-client's shared/identity.ts already shows elsewhere in
    // the app, so a raw HTTP caller bypassing the UI can never claim a name that could be mistaken
    // for a real "username#discriminator" account. See resolveUserName() below for why this is
    // derived, not client-supplied.
    private static final String GUEST_NAME_PREFIX = "FupaTroopa#";
    private static final int GUEST_NAME_RANGE = 900_000;
    private static final int GUEST_NAME_OFFSET = 100_000;

    private final LatenessPredictionService predictionService;
    private final IdentityResolver identityResolver;

    @PostMapping
    public ResponseEntity<LatenessPrediction> submitPrediction(
            @RequestBody LatenessPredictionRequest request,
            Authentication authentication
    ) {
        String identity = identityResolver.resolve(authentication, request.getUserToken());
        String userName = resolveUserName(authentication, identity);
        return ResponseEntity.ok(predictionService.submitPrediction(request, identity, userName));
    }

    /**
     * The displayed name is never taken from the request body — a logged-in user could otherwise
     * spoof another account's exact "username#discriminator" string, and a guest could otherwise
     * impersonate one too. Logged-in callers get their real identity; guests get a name derived
     * from their guest token via String.hashCode() (JLS-specified, stable forever), so the same
     * guest always sees the same name and it matches the "Voting As" preview stats-client computes
     * client-side with the equivalent hash (see identity.ts's resolveVoteDisplayName).
     */
    private static String resolveUserName(Authentication authentication, String identity) {
        if (authentication != null && authentication.getPrincipal() instanceof AppPrincipal principal) {
            return principal.getUsername() + "#" + principal.getDiscriminator();
        }
        return GUEST_NAME_PREFIX + (Math.floorMod(identity.hashCode(), GUEST_NAME_RANGE) + GUEST_NAME_OFFSET);
    }

    @GetMapping("/leaderboard/latest")
    public ResponseEntity<Page<LatenessPredictionLeaderboardEntry>> getLatestLeaderboard(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "proximityScore", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ResponseEntity.ok(predictionService.getLatestLeaderboard(search, pageable));
    }
}
