package com.h3late.stats.service;

import com.h3late.stats.entity.AppUser;
import com.h3late.stats.entity.AuthProvider;
import com.h3late.stats.entity.LinkedIdentity;
import com.h3late.stats.repository.AppUserRepository;
import com.h3late.stats.repository.LinkedIdentityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AppUserFactory {

    private final AppUserRepository appUserRepo;
    private final LinkedIdentityRepository linkedIdentityRepo;

    /**
     * Throws DataIntegrityViolationException on a concurrent-signup collision (two requests
     * racing to create the same provider identity) — AccountService.upsertFromProvider catches
     * this and falls back to touchLastLogin for whichever request lost the race.
     */
    @Transactional
    public AppUser createWithIdentity(AuthProvider provider, String providerUserId, String displayName, String email, String avatarUrl) {
        AppUser user = appUserRepo.save(AppUser.builder()
                .username(displayName)
                .email(email)
                .avatarUrl(avatarUrl)
                .lastLoginAt(Instant.now())
                .build());

        linkedIdentityRepo.save(LinkedIdentity.builder()
                .userId(user.getId())
                .provider(provider)
                .providerUserId(providerUserId)
                .build());

        return user;
    }
}
