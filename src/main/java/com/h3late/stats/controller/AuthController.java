package com.h3late.stats.controller;

import com.h3late.stats.dto.AuthMeResponse;
import com.h3late.stats.security.AppPrincipal;
import com.h3late.stats.service.GameTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final GameTokenService gameTokenService;

    @GetMapping("/me")
    public AuthMeResponse me(@AuthenticationPrincipal AppPrincipal principal) {
        if (principal == null) {
            return AuthMeResponse.anonymous();
        }
        return new AuthMeResponse(
                true,
                principal.getUserId(),
                principal.getUsername(),
                principal.getDiscriminator(),
                principal.getEmail(),
                principal.getAvatarUrl());
    }

    // Requires an authenticated session (enforced in SecurityConfig) — principal is never null
    // here. Guests never call this endpoint at all, so game-server play stays unaffected.
    @GetMapping("/game-token")
    public GameTokenService.GameToken gameToken(@AuthenticationPrincipal AppPrincipal principal) {
        return gameTokenService.issue(principal.getUserId(), principal.getUsername(), principal.getDiscriminator());
    }
}
