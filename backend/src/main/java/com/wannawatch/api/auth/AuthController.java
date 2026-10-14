package com.wannawatch.api.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AccountService accounts;
    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final boolean googleSignInEnabled;

    public AuthController(
            AccountService accounts,
            AuthenticationManager authenticationManager,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            @Value("${app.google-sign-in-enabled}") boolean googleSignInEnabled) {
        this.accounts = accounts;
        this.authenticationManager = authenticationManager;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.googleSignInEnabled = googleSignInEnabled;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken());
    }

    @GetMapping("/google")
    public Map<String, Boolean> googleAvailability() {
        return Map.of("enabled", googleSignInEnabled);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> register(@Valid @RequestBody RegistrationRequest request) {
        accounts.register(request.email(), request.displayName(), request.password());
        return Map.of("message", "Check your email for a verification link.");
    }

    @PostMapping("/verify")
    public Map<String, String> verify(@Valid @RequestBody VerificationRequest request) {
        accounts.verifyEmail(request.token());
        return Map.of("message", "Your email is verified. You can now sign in.");
    }

    @PostMapping("/login")
    public AccountResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        sessionAuthenticationStrategy.onAuthentication(authentication, servletRequest, servletResponse);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return AccountResponse.from((UserAccount) authentication.getPrincipal());
    }

    @GetMapping("/me")
    public AccountResponse currentAccount(Authentication authentication) {
        String email = authentication.getPrincipal() instanceof OAuth2User oauthUser
                ? oauthUser.getAttribute("email")
                : authentication.getName();
        return accounts.accountForEmail(email);
    }

    @ExceptionHandler(AccountService.AccountAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> duplicateAccount() {
        return Map.of("message", "An account already exists for this email address.");
    }

    @ExceptionHandler(AccountService.InvalidPasswordException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalidPassword() {
        return Map.of("message", "Choose a password with no more than 72 UTF-8 bytes.");
    }

    @ExceptionHandler(AccountService.InvalidVerificationTokenException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalidVerification() {
        return Map.of("message", "This verification link is invalid, expired, or already used.");
    }

    @ExceptionHandler(AccountService.VerificationEmailException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, String> emailUnavailable() {
        return Map.of("message", "The verification email could not be sent. Please try again.");
    }

    @ExceptionHandler(AccountService.AccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> accountNotFound() {
        return Map.of("message", "Account not found.");
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> invalidCredentials() {
        return Map.of("message", "Email or password is incorrect, or the email has not been verified.");
    }

    public record RegistrationRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 120) String displayName,
            @NotBlank @Size(min = 12, max = 72) String password) {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password) {
    }

    public record VerificationRequest(@NotBlank @Size(max = 128) String token) {
    }
}
