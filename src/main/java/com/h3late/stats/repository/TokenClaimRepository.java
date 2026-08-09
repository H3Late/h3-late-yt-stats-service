package com.h3late.stats.repository;

import com.h3late.stats.entity.TokenClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenClaimRepository extends JpaRepository<TokenClaim, Long> {

    Optional<TokenClaim> findByToken(String token);

    boolean existsByUserId(Long userId);
}
