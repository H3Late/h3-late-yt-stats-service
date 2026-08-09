package com.h3late.stats.service;

import com.h3late.stats.entity.AppUser;
import com.h3late.stats.entity.AuthProvider;
import com.h3late.stats.entity.LinkedIdentity;
import com.h3late.stats.repository.AppUserRepository;
import com.h3late.stats.repository.LinkedIdentityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AppUserRepository appUserRepo;
    private final LinkedIdentityRepository linkedIdentityRepo;
    private final AppUserFactory appUserFactory;

    public record UpsertResult(AppUser user, boolean newAccount) {
    }

    /**
     * Provider-agnostic upsert: called from a Google-specific OidcUserService today, and from a
     * Discord/other provider's OAuth2UserService later — neither this method nor AppUser/LinkedIdentity
     * need to change to add a second provider.
     */
    public UpsertResult upsertFromProvider(AuthProvider provider, String providerUserId, String displayName, String email, String avatarUrl) {
        Optional<LinkedIdentity> existing = linkedIdentityRepo.findByProviderAndProviderUserId(provider, providerUserId);
        if (existing.isPresent()) {
            return new UpsertResult(touchLastLogin(existing.get().getUserId()), false);
        }

        try {
            return new UpsertResult(appUserFactory.createWithIdentity(provider, providerUserId, displayName, email, avatarUrl), true);
        } catch (DataIntegrityViolationException e) {
            // Lost a concurrent race to create this identity — the winning request already exists.
            return linkedIdentityRepo.findByProviderAndProviderUserId(provider, providerUserId)
                    .map(link -> new UpsertResult(touchLastLogin(link.getUserId()), false))
                    .orElseThrow(() -> e);
        }
    }

    private AppUser touchLastLogin(Long userId) {
        AppUser user = appUserRepo.findById(userId)
                .orElseThrow(() -> new IllegalStateException("LinkedIdentity references missing AppUser id=" + userId));
        user.setLastLoginAt(Instant.now());
        return appUserRepo.save(user);
    }
}
