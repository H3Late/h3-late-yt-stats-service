package com.h3late.stats.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * Wraps the OidcUser Spring Security produces on login with our own AppUser identity, so
 * downstream code (IdentityResolver, /api/auth/me, /api/auth/game-token) can read the resolved
 * userId/username/discriminator straight off the principal without a second DB round trip.
 *
 * Must stay cleanly Serializable: Spring Session JDBC persists the whole SecurityContext
 * (including this principal) into a BYTEA column via Java serialization.
 */
public class AppPrincipal implements OidcUser, Serializable {

    private final OidcUser delegate;
    private final Long userId;
    private final String username;
    private final String discriminator;
    private final String email;
    private final String avatarUrl;
    private final boolean newAccount;

    public AppPrincipal(OidcUser delegate, Long userId, String username, String discriminator, String email, String avatarUrl, boolean newAccount) {
        this.delegate = delegate;
        this.userId = userId;
        this.username = username;
        this.discriminator = discriminator;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.newAccount = newAccount;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getDiscriminator() {
        return discriminator;
    }

    public String getEmail() {
        return email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public boolean isNewAccount() {
        return newAccount;
    }

    @Override
    public Map<String, Object> getClaims() {
        return delegate.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return delegate.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return delegate.getIdToken();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return delegate.getAuthorities();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }
}
