package com.h3late.stats.security;

/**
 * Reserved-shape marker for a resolved account identity, as opposed to a client-supplied guest
 * token. Guest tokens are always UUIDs (crypto.randomUUID() on the frontend), which can never
 * start with "u:" — so this prefix is only ever produced server-side from an authenticated
 * session, never accepted as client input (see IdentityResolver.resolve()).
 */
public final class AccountIdentity {

    private static final String PREFIX = "u:";

    private AccountIdentity() {
    }

    public static String of(Long userId) {
        return PREFIX + userId;
    }

    public static boolean isAccountIdentity(String value) {
        return value != null && value.startsWith(PREFIX);
    }
}
