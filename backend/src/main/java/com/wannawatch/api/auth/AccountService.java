package com.wannawatch.api.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserAccountRepository accounts;
    private final EmailVerificationTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final String frontendUrl;
    private final String mailFrom;

    public AccountService(
            UserAccountRepository accounts,
            EmailVerificationTokenRepository tokens,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.mail-from}") String mailFrom) {
        this.accounts = accounts;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
        this.mailFrom = mailFrom;
    }

    @Transactional
    public void register(String email, String displayName, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (accounts.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new AccountAlreadyExistsException();
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidPasswordException();
        }

        UserAccount account = accounts.save(new UserAccount(
                normalizedEmail, displayName.trim(), passwordEncoder.encode(password), false));
        String rawToken = newVerificationToken();
        tokens.save(new EmailVerificationToken(
                account, hashToken(rawToken), Instant.now().plus(24, ChronoUnit.HOURS)));

        String verificationUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .queryParam("verify", rawToken)
                .build()
                .encode()
                .toUriString();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(normalizedEmail);
        message.setSubject("Verify your Wanna Watch account");
        message.setText("Hello " + displayName.trim() + ",\n\n"
                + "Verify your email address to finish creating your account:\n"
                + verificationUrl + "\n\n"
                + "This link expires in 24 hours. If you did not create this account, you can ignore this email.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new VerificationEmailException(exception);
        }
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        EmailVerificationToken token = tokens.findByTokenHashAndUsedAtIsNull(hashToken(rawToken))
                .filter(candidate -> candidate.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(InvalidVerificationTokenException::new);
        token.getUser().verifyEmail();
        token.markUsed();
    }

    @Transactional(readOnly = true)
    public AccountResponse accountForEmail(String email) {
        return accounts.findByEmailIgnoreCase(email)
                .map(AccountResponse::from)
                .orElseThrow(AccountNotFoundException::new);
    }

    @Transactional
    public UserAccount findOrCreateGoogleAccount(String email, String displayName) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        return accounts.findByEmailIgnoreCase(normalizedEmail)
                .map(account -> {
                    if (!account.isEmailVerified()) {
                        throw new UnverifiedGoogleAccountException();
                    }
                    return account;
                })
                .orElseGet(() -> accounts.save(new UserAccount(normalizedEmail, displayName, null, true)));
    }

    private static String newVerificationToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public static class AccountAlreadyExistsException extends RuntimeException {
    }

    public static class AccountNotFoundException extends RuntimeException {
    }

    public static class InvalidPasswordException extends RuntimeException {
    }

    public static class InvalidVerificationTokenException extends RuntimeException {
    }

    public static class VerificationEmailException extends RuntimeException {
        public VerificationEmailException(Throwable cause) {
            super(cause);
        }
    }

    public static class UnverifiedGoogleAccountException extends RuntimeException {
    }
}
