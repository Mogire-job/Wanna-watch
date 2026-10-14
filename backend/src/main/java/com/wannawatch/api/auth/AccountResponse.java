package com.wannawatch.api.auth;

public record AccountResponse(String email, String displayName, boolean emailVerified) {
    public static AccountResponse from(UserAccount account) {
        return new AccountResponse(account.getEmail(), account.getDisplayName(), account.isEmailVerified());
    }
}
