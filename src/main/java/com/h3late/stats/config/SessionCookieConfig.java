package com.h3late.stats.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Blank domain (the default) leaves the cookie scoped to the exact host serving the response —
 * needed for local dev (localhost) where a ".h3late.com" domain attribute would be rejected by
 * the browser. Set SESSION_COOKIE_DOMAIN in production if frontend/API ever share a real parent
 * domain (e.g. app.h3late.com and api.h3late.com) — leave unset while frontend/backend are on
 * Railway-generated *.up.railway.app URLs, since that's a public suffix (each service gets an
 * unrelated subdomain, not one you could scope a cookie across).
 * -
 * SameSite default is None, not Lax: frontend and backend are separate registrable domains on
 * Railway, so this is a genuinely cross-site cookie from the browser's perspective — Lax is only
 * sent on top-level navigations, not on the frontend's fetch()/XHR calls to the API, so it would
 * silently never be sent there, breaking login. Browsers require Secure=true whenever
 * SameSite=None, which is why that's the default too.
 * -
 * Local HTTP dev exception: if frontend/backend both run on plain <a href="http://localhost">...</a> (different
 * ports), they're same-site (SameSite compares registrable domain + scheme, not port), AND
 * SameSite=None cookies are rejected outright over plain HTTP (no TLS). So for that setup only,
 * override both SESSION_COOKIE_SAME_SITE=Lax and SESSION_COOKIE_SECURE=false.
 */
@Configuration
public class SessionCookieConfig {

    @Value("${session.cookie.domain:}")
    private String cookieDomain;

    @Value("${session.cookie.secure:true}")
    private boolean secure;

    @Value("${session.cookie.same-site:None}")
    private String sameSite;

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        if (!cookieDomain.isBlank()) {
            serializer.setDomainName(cookieDomain);
        }
        serializer.setSameSite(sameSite);
        serializer.setUseSecureCookie(secure);
        return serializer;
    }
}
