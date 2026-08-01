package com.h3late.stats.service;

import com.h3late.stats.entity.AppUser;
import com.h3late.stats.entity.AuthProvider;
import com.h3late.stats.entity.LinkedIdentity;
import com.h3late.stats.repository.AppUserRepository;
import com.h3late.stats.repository.LinkedIdentityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * Isolated in its own bean (rather than a method on AccountService) so createAttempt can run in
 * its own REQUIRES_NEW transaction per retry. Postgres aborts the whole transaction on the first
 * constraint violation, so retrying the insert within the same transaction as a prior failed
 * attempt would just fail again immediately with "current transaction is aborted" — each attempt
 * needs a fresh transaction. REQUIRES_NEW only takes effect via a proxied call from another bean,
 * so this can't just be a private method looped over from within the same class.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppUserFactory {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_ATTEMPTS = 10;

    private final AppUserRepository appUserRepo;
    private final LinkedIdentityRepository linkedIdentityRepo;

    public AppUser createWithIdentity(AuthProvider provider, String providerUserId, String displayName, String email, String avatarUrl) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return createAttempt(provider, providerUserId, displayName, email, avatarUrl);
            } catch (DataIntegrityViolationException e) {
                log.warn("Collision creating account for provider={} on attempt {}/{}, retrying", provider, attempt, MAX_ATTEMPTS);
            }
        }
        throw new IllegalStateException("Failed to create account for provider=" + provider + " after " + MAX_ATTEMPTS + " attempts");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AppUser createAttempt(AuthProvider provider, String providerUserId, String displayName, String email, String avatarUrl) {
        AppUser user = appUserRepo.save(AppUser.builder()
                .username(displayName)
                .discriminator(generateDiscriminator())
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

    private String generateDiscriminator() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
