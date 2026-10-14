package com.wannawatch.api.auth;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class GoogleAccountUserService extends DefaultOAuth2UserService {

    private final AccountService accounts;

    public GoogleAccountUserService(AccountService accounts) {
        this.accounts = accounts;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(userRequest);
        String email = user.getAttribute("email");
        Boolean verified = user.getAttribute("email_verified");
        String displayName = user.getAttribute("name");
        if (email == null || !Boolean.TRUE.equals(verified)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("unverified_google_email"), "Google did not verify the email address");
        }
        try {
            accounts.findOrCreateGoogleAccount(email, displayName == null ? "" : displayName);
        } catch (AccountService.UnverifiedGoogleAccountException exception) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_verification_required"),
                    "Verify this email address before signing in with Google");
        }
        return user;
    }
}
