
package com.breadbitt.service.impl;

import com.breadbitt.domain.ExternalIdentity;
import com.breadbitt.domain.User;
import com.breadbitt.repository.ExternalIdentityRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExternalIdentityServiceTest {

    private ExternalIdentityRepository repository;
    private ExternalIdentityService service;

    private static final String ISSUER = "http://localhost:8180/realms/breadbite";

    private static final String SUBJECT = "keycloak-test-subject-123";

    @BeforeEach
    void setUp() {
        repository = mock(ExternalIdentityRepository.class);
        service = new ExternalIdentityService(repository);
    }

    @Test
    void shouldResolveLinkedEnabledUser() {
        User user = new User();
        user.setId(101L);
        user.setUsername("breadbite-test");
        user.setEnabled(true);

        ExternalIdentity identity = new ExternalIdentity(ISSUER, SUBJECT, user);

        when(repository.findByIssuerAndSubject(ISSUER, SUBJECT))
                .thenReturn(Optional.of(identity));

        User result = service.resolveUser(ISSUER, SUBJECT);

        assertSame(user, result);

        verify(repository).findByIssuerAndSubject(ISSUER, SUBJECT);
    }

    @Test
    void shouldRejectUnlinkedIdentity() {
        when(repository.findByIssuerAndSubject(ISSUER, SUBJECT))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> service.resolveUser(ISSUER, SUBJECT));
    }

    @Test
    void shouldRejectDisabledUser() {
        User user = new User();
        user.setEnabled(false);

        ExternalIdentity identity = new ExternalIdentity(ISSUER, SUBJECT, user);

        when(repository.findByIssuerAndSubject(ISSUER, SUBJECT))
                .thenReturn(Optional.of(identity));

        assertThrows(
                AccessDeniedException.class,
                () -> service.resolveUser(ISSUER, SUBJECT));
    }

    @Test
    void shouldRejectMissingIssuer() {
        assertThrows(
                AccessDeniedException.class,
                () -> service.resolveUser(null, SUBJECT));

        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectMissingSubject() {
        assertThrows(
                AccessDeniedException.class,
                () -> service.resolveUser(ISSUER, " "));

        verifyNoInteractions(repository);
    }
}
