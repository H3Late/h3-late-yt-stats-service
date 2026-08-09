package com.h3late.stats.repository;

import com.h3late.stats.entity.AuthProvider;
import com.h3late.stats.entity.LinkedIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LinkedIdentityRepository extends JpaRepository<LinkedIdentity, Long> {

    Optional<LinkedIdentity> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);
}
