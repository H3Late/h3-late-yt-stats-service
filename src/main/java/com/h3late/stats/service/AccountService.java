package com.h3late.stats.service;

import com.h3late.stats.entity.AppUser;
import com.h3late.stats.entity.AuthProvider;
import com.h3late.stats.entity.LinkedIdentity;
import com.h3late.stats.repository.AppUserRepository;
import com.h3late.stats.repository.LinkedIdentityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AppUserRepository appUserRepo;
    private final LinkedIdentityRepository linkedIdentityRepo;
    private final AppUserFactory appUserFactory;

    /**
     * Provider-agnostic upsert: called from a Google-specific OidcUserService today, and from a
     * Discord/other provider's OAuth2UserService later — neither this method nor AppUser/LinkedIdentity
     * need to change to add a second provider.
     */
    public AppUser upsertFromProvider(AuthProvider provider, String providerUserId, String displayName, String email, String avatarUrl) {
        Optional<LinkedIdentity> existing = linkedIdentityRepo.findByProviderAndProviderUserId(provider, providerUserId);
        if (existing.isPresent()) {
            return touchLastLogin(existing.get().getUserId());
        }

        try {
            return appUserFactory.createWithIdentity(provider, providerUserId, displayName, email, avatarUrl);
        } catch (IllegalStateException exhausted) {
            // Retries were exhausted — check whether that's because a concurrent request for the
            // same provider identity won the race in the meantime (not a discriminator collision).
            return linkedIdentityRepo.findByProviderAndProviderUserId(provider, providerUserId)
                    .map(link -> touchLastLogin(link.getUserId()))
                    .orElseThrow(() -> exhausted);
        }
    }

    private AppUser touchLastLogin(Long userId) {
        AppUser user = appUserRepo.findById(userId)
                .orElseThrow(() -> new IllegalStateException("LinkedIdentity references missing AppUser id=" + userId));
        user.setLastLoginAt(Instant.now());
        return appUserRepo.save(user);
    }
}
