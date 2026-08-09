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
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
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
                // The plain (non-XOR) request handler keeps that simple read-cookie-echo-header
                // pattern working without deferred-token ceremony.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        // Existing clip-contest endpoints authenticate via opaque request-body
                        // tokens, not cookies, so CSRF (which protects ambient cookie credentials)
                        // doesn't apply to them and requiring it would break today's frontend.
                        .ignoringRequestMatchers("/api/contest/**"))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/game-token").authenticated()
                        .anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(appOidcUserService))
                        .successHandler(claimOnFirstLoginSuccessHandler()))
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
            String separator = postLoginUrl.contains("?") ? "&" : "?";
            String targetUrl = linkedSomething ? postLoginUrl + separator + "linked=true" : postLoginUrl;
            response.sendRedirect(targetUrl);
        };
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
