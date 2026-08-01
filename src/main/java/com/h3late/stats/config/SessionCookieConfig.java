package com.h3late.stats.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Blank domain (the default) leaves the cookie scoped to the exact host serving the response —
 * needed for local dev (localhost) where a ".h3late.com" domain attribute would be rejected by
 * the browser. Set SESSION_COOKIE_DOMAIN in production so the session cookie is shared across
 * subdomains (e.g. app.h3late.com and api.h3late.com).
 */
@Configuration
public class SessionCookieConfig {

    @Value("${session.cookie.domain:}")
    private String cookieDomain;

    @Value("${session.cookie.secure:true}")
    private boolean secure;

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        if (!cookieDomain.isBlank()) {
            serializer.setDomainName(cookieDomain);
        }
        serializer.setSameSite("Lax");
        serializer.setUseSecureCookie(secure);
        return serializer;
    }
}
