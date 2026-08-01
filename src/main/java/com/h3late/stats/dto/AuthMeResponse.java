package com.h3late.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthMeResponse {
    private boolean authenticated;
    private Long userId;
    private String username;
    private String discriminator;
    private String email;
    private String avatarUrl;

    public static AuthMeResponse anonymous() {
        return new AuthMeResponse(false, null, null, null, null, null);
    }
}
