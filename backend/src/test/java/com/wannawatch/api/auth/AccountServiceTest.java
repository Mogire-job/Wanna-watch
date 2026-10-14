package com.wannawatch.api.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    private final UserAccountRepository accounts = mock(UserAccountRepository.class);
    private final EmailVerificationTokenRepository tokens = mock(EmailVerificationTokenRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final AccountService service = new AccountService(
            accounts, tokens, encoder, mailSender, "http://localhost:5174", "no-reply@example.com");

    @Test
    void registrationNormalizesEmailHashesPasswordAndSendsVerificationLink() {
        when(accounts.findByEmailIgnoreCase("avery@example.com")).thenReturn(Optional.empty());
        when(accounts.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.register(" Avery@Example.com ", "Avery", "a-strong-password-123");

        var accountCaptor = org.mockito.ArgumentCaptor.forClass(UserAccount.class);
        verify(accounts).save(accountCaptor.capture());
        UserAccount saved = accountCaptor.getValue();
        assertEquals("avery@example.com", saved.getEmail());
        assertFalse(saved.isEmailVerified());
        assertNotEquals("a-strong-password-123", saved.getPassword());
        assertTrue(encoder.matches("a-strong-password-123", saved.getPassword()));
        verify(tokens).save(any(EmailVerificationToken.class));
        var messageCaptor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        assertEquals("avery@example.com", messageCaptor.getValue().getTo()[0]);
        assertTrue(messageCaptor.getValue().getText().contains("?verify="));
    }

    @Test
    void verificationEnablesAccountAndConsumesTheToken() {
        UserAccount account = new UserAccount("avery@example.com", "Avery", "encoded", false);
        EmailVerificationToken token = new EmailVerificationToken(
                account, "hash", Instant.now().plus(1, ChronoUnit.HOURS));
        when(tokens.findByTokenHashAndUsedAtIsNull(anyString())).thenReturn(Optional.of(token));

        service.verifyEmail("one-time-token");

        assertTrue(account.isEmailVerified());
        assertNotNull(token.getUsedAt());
    }

    @Test
    void duplicateAccountCannotRegisterAgain() {
        when(accounts.findByEmailIgnoreCase("avery@example.com"))
                .thenReturn(Optional.of(new UserAccount("avery@example.com", "Avery", "encoded", false)));

        assertThrows(AccountService.AccountAlreadyExistsException.class,
                () -> service.register("avery@example.com", "Avery", "a-strong-password-123"));
        verifyNoInteractions(mailSender);
        verify(tokens, never()).save(any());
    }
}
