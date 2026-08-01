package com.h3late.stats.security;

import com.h3late.stats.entity.AppUser;
import com.h3late.stats.entity.AuthProvider;
import com.h3late.stats.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppOidcUserService extends OidcUserService {

    private final AccountService accountService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String providerUserId = oidcUser.getSubject();
        String displayName = oidcUser.getFullName() != null ? oidcUser.getFullName() : oidcUser.getEmail();

        AppUser user = accountService.upsertFromProvider(
                AuthProvider.GOOGLE,
                providerUserId,
                displayName,
                oidcUser.getEmail(),
                oidcUser.getPicture());

        return new AppPrincipal(oidcUser, user.getId(), user.getUsername(), user.getDiscriminator(), user.getEmail(), user.getAvatarUrl());
    }
}
