package com.h3late.stats.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Folds logged-in identity into the existing token-based columns (userToken/voterToken/
 * reporterToken) rather than changing their meaning: logged-in callers resolve to a canonical
 * "u:<id>" string, guests keep using their raw localStorage token. Both flow into the exact same
 * repository query methods and DB unique constraints that existed before accounts did.
 */
@Component
public class IdentityResolver {

    public String resolve(Authentication authentication, String fallbackToken) {
        Long userId = resolveUserId(authentication);
        if (userId != null) {
            return "u:" + userId;
        }
        if (fallbackToken == null || fallbackToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token is required");
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
