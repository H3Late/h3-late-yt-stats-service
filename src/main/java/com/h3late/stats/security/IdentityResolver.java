package com.h3late.stats.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Produces the single value stored in ContestClip/ClipVote/ClipReport's userId column: a
 * canonical "u:<id>" string for logged-in callers (see AccountIdentity), or the caller's raw
 * guest token otherwise. Both flow into the exact same repository query methods and DB unique
 * constraints — logged-in vs. guest is just a different source for the same identity value.
 */
@Component
public class IdentityResolver {

    public String resolve(Authentication authentication, String fallbackToken) {
        Long userId = resolveUserId(authentication);
        if (userId != null) {
            return AccountIdentity.of(userId);
        }
        if (fallbackToken == null || fallbackToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token is required");
        }
        // Guest tokens are always UUIDs and can never legitimately take this shape — reject
        // rather than let an unauthenticated caller inject/spoof a resolved account identity.
        if (AccountIdentity.isAccountIdentity(fallbackToken)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token");
        }
        return fallbackToken;
    }

    public Long resolveUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        if (authentication.getPrincipal() instanceof AppPrincipal principal) {
            return principal.getUserId();
        }
        return null;
    }
}
