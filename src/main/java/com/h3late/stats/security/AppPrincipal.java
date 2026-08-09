package com.h3late.stats.security;

import lombok.Getter;
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

    private static final long serialVersionUID = 1L;

    // Not exposed via @Getter — nothing outside this class should reach through to the raw
    // delegate; the OidcUser interface methods below already forward what's needed from it.
    // Concrete OidcUser implementations Spring hands us here (e.g. DefaultOidcUser) are
    // Serializable at runtime even though the field's static type (an interface) isn't
    // guaranteed to be — that's exactly what javac's [serial] lint warning is flagging.
    private final OidcUser delegate;

    @Getter
    private final Long userId;
    @Getter
    private final String username;
    @Getter
    private final String discriminator;
    @Getter
    private final String email;
    @Getter
    private final String avatarUrl;
    @Getter
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
