package com.h3late.stats.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

import java.time.Duration;

/**
 * @EnableJdbcHttpSession is what actually activates Spring Session JDBC — the
 * spring-session-jdbc dependency plus spring.session.* properties in application.yaml are not
 * enough on their own. Confirmed the hard way: without this annotation present anywhere, no
 * `spring_session`/`spring_session_attributes` tables ever got created despite
 * `initialize-schema: always`, because the configuration class that reads that property is only
 * registered when this annotation triggers it. Every "session" up to that point was actually
 * Tomcat's own native in-memory HttpSession (cookie name JSESSIONID) — invisible to Postgres,
 * and not shared across multiple autoscaled Railway instances, silently defeating the entire
 * point of using Spring Session JDBC here.
 *
 * Blank domain (the default) leaves the cookie scoped to the exact host serving the response —
 * needed for local dev (localhost) where a ".h3late.com" domain attribute would be rejected by
 * the browser. Set SESSION_COOKIE_DOMAIN in production if frontend/API ever share a real parent
 * domain (e.g. app.h3late.com and api.h3late.com) — leave unset while frontend/backend are on
 * Railway-generated *.up.railway.app URLs, since that's a public suffix (each service gets an
 * unrelated subdomain, not one you could scope a cookie across).
 *
 * SameSite default is None, not Lax: frontend and backend are separate registrable domains on
 * Railway, so this is a genuinely cross-site cookie from the browser's perspective — Lax is only
 * sent on top-level navigations, not on the frontend's fetch()/XHR calls to the API, so it would
 * silently never be sent there, breaking login. Browsers require Secure=true whenever
 * SameSite=None, which is why that's the default too.
 *
 * Local HTTP dev exception: if frontend/backend both run on plain http://localhost (different
 * ports), they're same-site (SameSite compares registrable domain + scheme, not port), AND
 * SameSite=None cookies are rejected outright over plain HTTP (no TLS). So for that setup only,
 * override both SESSION_COOKIE_SAME_SITE=Lax and SESSION_COOKIE_SECURE=false.
 */
@Configuration
@EnableJdbcHttpSession
public class SessionCookieConfig {

    @Value("${session.cookie.domain:}")
    private String cookieDomain;

    @Value("${session.cookie.secure:true}")
    private boolean secure;

    @Value("${session.cookie.same-site:None}")
    private String sameSite;

    // Single source of truth for how long a session stays valid, applied below to both the
    // cookie's Max-Age and the actual JDBC session repository's expiry (via the customizer bean),
    // so the two can't drift apart. @EnableJdbcHttpSession's own maxInactiveIntervalInSeconds
    // attribute can't reference this property directly (it's a plain int, no placeholder support),
    // which is exactly why the customizer bean exists instead of just setting that attribute.
    @Value("${spring.session.timeout:7d}")
    private Duration sessionTimeout;

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        if (!cookieDomain.isBlank()) {
            serializer.setDomainName(cookieDomain);
        }
        serializer.setSameSite(sameSite);
        serializer.setUseSecureCookie(secure);
        serializer.setCookieMaxAge((int) sessionTimeout.getSeconds());
        return serializer;
    }

    @Bean
    public SessionRepositoryCustomizer<JdbcIndexedSessionRepository> sessionRepositoryCustomizer() {
        return repository -> repository.setDefaultMaxInactiveInterval(sessionTimeout);
    }
}
