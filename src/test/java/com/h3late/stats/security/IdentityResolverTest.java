package com.h3late.stats.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

public class IdentityResolverTest {

    private IdentityResolver identityResolver;

    @BeforeEach
    public void setUp() {
        identityResolver = new IdentityResolver();
    }

    @Test
    public void resolve_withNullAuthentication_returnsFallbackToken() {
        assertEquals("guest-token", identityResolver.resolve(null, "guest-token"));
    }

    @Test
    public void resolve_withAnonymousAuthentication_returnsFallbackToken() {
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

        assertEquals("guest-token", identityResolver.resolve(anonymous, "guest-token"));
    }

    @Test
    public void resolve_withNullAuthentication_andBlankFallback_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> identityResolver.resolve(null, ""));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    public void resolve_withAuthenticatedAppPrincipal_returnsCanonicalUserId() {
        Authentication authentication = authenticatedAs(42L);

        assertEquals("u:42", identityResolver.resolve(authentication, "guest-token"));
    }

    @Test
    public void resolveUserId_withAuthenticatedAppPrincipal_returnsUserId() {
        Authentication authentication = authenticatedAs(7L);

        assertEquals(7L, identityResolver.resolveUserId(authentication));
    }

    @Test
    public void resolveUserId_withAnonymousAuthentication_returnsNull() {
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

        assertNull(identityResolver.resolveUserId(anonymous));
    }

    private Authentication authenticatedAs(Long userId) {
        AppPrincipal principal = new AppPrincipal(null, userId, "Danny", "482913", "danny@example.com", null);
        Authentication authentication = Mockito.mock(Authentication.class);
        Mockito.when(authentication.isAuthenticated()).thenReturn(true);
        Mockito.when(authentication.getPrincipal()).thenReturn(principal);
        return authentication;
    }
}
