package com.h3late.stats.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * CsrfTokenRequestAttributeHandler only stores a *lazy* supplier as the "_csrf" request attribute
 * (confirmed by decompiling CsrfTokenRequestAttributeHandler.handle() — it never calls
 * Supplier.get() itself). CsrfFilter only resolves that supplier — which is what actually writes
 * the XSRF-TOKEN cookie — for requests it's validating, i.e. unsafe methods to non-ignored paths.
 * A pure REST API has nothing that ever touches the attribute on a GET (no server-rendered view
 * reads `_csrf.token`), so the cookie was never being issued during the OAuth login flow at all —
 * meaning a fresh browser's very first state-changing request (e.g. the first logout after
 * login) was guaranteed to 403, since no cookie existed yet to echo back as a header.
 *
 * Forcing resolution on every request (GET included) via getToken() makes the cookie appear as
 * early as the first page load / first /api/auth/me call. This is Spring Security's own
 * documented pattern for CSRF + single-page applications, meant to be paired with the plain
 * (non-XOR) CsrfTokenRequestAttributeHandler already configured in SecurityConfig.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
