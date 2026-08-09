package com.h3late.stats.security;

import com.h3late.stats.service.AccountClaimService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ANON_TOKEN_COOKIE_NAME = "anon_token";

    private final CorsConfigurationSource corsConfigurationSource;
    private final AppOidcUserService appOidcUserService;
    private final AccountClaimService accountClaimService;

    @Value("${frontend.post-login-url}")
    private String postLoginUrl;

    // Same property SessionCookieConfig uses for the SESSION cookie. The XSRF-TOKEN cookie needs
    // it too: the double-submit CSRF pattern requires frontend JS to read this cookie via
    // document.cookie and echo it back as a header, and document.cookie only exposes cookies
    // scoped to the current page's host. Host-only (no Domain) is fine for SESSION, since the
    // browser auto-attaches that one to matching requests without JS ever reading it — but it
    // breaks XSRF-TOKEN as soon as frontend and API are on different subdomains.
    @Value("${session.cookie.domain:}")
    private String cookieDomain;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // Frontend reads the XSRF-TOKEN cookie and echoes it back as a header on
                // state-changing requests to session-bearing endpoints (currently just
                // /api/auth/logout — claiming is no longer a client-triggered endpoint at all).
                // The plain (non-XOR) request handler avoids the BREACH-mitigation encoding the
                // XOR variant applies — without it, the raw cookie value wouldn't match what a
                // simple "read cookie, echo as header" SPA is expected to send.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        // Existing clip-contest endpoints authenticate via opaque request-body
                        // tokens, not cookies, so CSRF (which protects ambient cookie credentials)
                        // doesn't apply to them and requiring it would break today's frontend.
                        .ignoringRequestMatchers("/api/contest/**"))
                // CsrfTokenRequestAttributeHandler only stores a *lazy* token supplier as a
                // request attribute — CsrfFilter only resolves it (which is what actually writes
                // the XSRF-TOKEN cookie) for requests it's validating, i.e. unsafe methods to
                // non-ignored paths. A pure REST API has no server-rendered view to otherwise
                // force that resolution on a GET, so without this filter the cookie is never
                // issued until the first state-changing request — meaning a fresh browser's
                // first POST (e.g. the first logout after login) is guaranteed to 403 with no
                // cookie yet to echo back. Forces resolution on every request instead.
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/game-token").authenticated()
                        .anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(appOidcUserService))
                        .successHandler(claimOnFirstLoginSuccessHandler())
                        .failureHandler(loginFailureHandler()))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()))
                // Without this, an unauthenticated request to an `authenticated()` API endpoint
                // would 302-redirect to Google's consent screen — meaningless to a fetch() caller.
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                request -> request.getRequestURI().startsWith("/api/")));

        return http.build();
    }

    /**
     * Runs once per login. On a brand-new account, reads the anon-token cookie the OAuth redirect
     * navigation carried along (set by the frontend, see stats-client's useVoterToken.ts) and
     * claims that browser's guest history atomically, server-side — no separate API call, no
     * window for anything to race, since this runs before the browser is ever redirected anywhere
     * it could act. Returning logins never attempt this (principal.isNewAccount() is false).
     */
    private AuthenticationSuccessHandler claimOnFirstLoginSuccessHandler() {
        return (request, response, authentication) -> {
            boolean linkedSomething = false;
            if (authentication.getPrincipal() instanceof AppPrincipal principal && principal.isNewAccount()) {
                String anonToken = readCookie(request, ANON_TOKEN_COOKIE_NAME);
                if (anonToken != null) {
                    AccountClaimService.ClaimResult result = accountClaimService.claimIfEligible(principal.getUserId(), anonToken);
                    linkedSomething = result.claimedAnything();
                }
            }
            String targetUrl = linkedSomething ? withParam(postLoginUrl, "linked", "true") : postLoginUrl;
            response.sendRedirect(targetUrl);
        };
    }

    /**
     * Covers both a user declining Google's consent screen and a genuine server-side failure
     * (e.g. account creation itself throwing) — either way, redirect back to the frontend with a
     * generic error signal instead of falling through to Spring Security's default /login?error,
     * which this backend doesn't serve anything for.
     */
    private AuthenticationFailureHandler loginFailureHandler() {
        return (request, response, exception) -> response.sendRedirect(withParam(postLoginUrl, "loginError", "true"));
    }

    private static String withParam(String url, String key, String value) {
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + key + "=" + value;
    }

    private static String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        // CookieCsrfTokenRepository has no public setCookieDomain in this Spring Security version
        // (7.0.3) — setCookieCustomizer is the supported way to set Domain on the cookie it emits.
        if (!cookieDomain.isBlank()) {
            repository.setCookieCustomizer(builder -> builder.domain(cookieDomain));
        }
        return repository;
    }
}
