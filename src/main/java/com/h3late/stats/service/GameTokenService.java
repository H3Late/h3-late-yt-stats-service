package com.h3late.stats.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Issues short-lived tokens that let an already-logged-in browser prove its identity to
 * game-server without game-server needing its own OAuth flow, user table, or DB — game-server
 * verifies the signature locally with the same shared secret (Railway env var), no network call
 * back here. HMAC (not RSA) is enough since both services are ours and share secrets via env vars
 * already (e.g. ADMIN_KEY).
 */
@Service
public class GameTokenService {

    private static final String AUDIENCE = "game-server";

    private final String secret;
    private final long ttlSeconds;

    public GameTokenService(
            @Value("${game.token.secret:}") String secret,
            @Value("${game.token.ttl-seconds:300}") long ttlSeconds
    ) {
        this.secret = secret;
        this.ttlSeconds = ttlSeconds;
    }

    public GameToken issue(Long userId, String username, String discriminator) {
        if (secret.isBlank()) {
            throw new IllegalStateException("GAME_TOKEN_SECRET is not configured");
        }

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(ttlSeconds);

        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("discriminator", discriminator)
                .audience().add(AUDIENCE).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();

        return new GameToken(token, expiresAt);
    }

    public record GameToken(String token, Instant expiresAt) {
    }
}
