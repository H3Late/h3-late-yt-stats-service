package com.h3late.stats.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final AppOidcUserService appOidcUserService;

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
                // state-changing requests to session-bearing endpoints (/api/auth/claim,
                // /api/auth/logout). The plain (non-XOR) request handler keeps that simple
                // read-cookie-echo-header pattern working without deferred-token ceremony.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        // Existing clip-contest endpoints authenticate via opaque request-body
                        // tokens, not cookies, so CSRF (which protects ambient cookie credentials)
                        // doesn't apply to them and requiring it would break today's frontend.
                        .ignoringRequestMatchers("/api/contest/**"))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/game-token", "/api/auth/claim").authenticated()
                        .anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(appOidcUserService))
                        .successHandler(postLoginSuccessHandler()))
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

    private SimpleUrlAuthenticationSuccessHandler postLoginSuccessHandler() {
        SimpleUrlAuthenticationSuccessHandler handler = new SimpleUrlAuthenticationSuccessHandler(postLoginUrl);
        handler.setAlwaysUseDefaultTargetUrl(true);
        return handler;
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
