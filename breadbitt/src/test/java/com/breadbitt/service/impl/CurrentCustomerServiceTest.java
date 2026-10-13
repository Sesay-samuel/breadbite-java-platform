
package com.breadbitt.service.impl;

import com.breadbitt.domain.User;
import com.breadbitt.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrentCustomerServiceTest {

    private static final String ISSUER = "http://localhost:8180/realms/breadbite";

    private static final String SUBJECT = "keycloak-customer-123";

    private UserService userService;
    private ExternalIdentityService externalIdentityService;
    private CurrentCustomerService currentCustomerService;

    @BeforeEach
    void setUp() {

        userService = mock(UserService.class);

        externalIdentityService = mock(ExternalIdentityService.class);

        currentCustomerService = new CurrentCustomerService(
                userService,
                externalIdentityService);
    }

    @Test
    void shouldResolveLegacyCustomer() {

        User user = new User();
        user.setUsername("breadbite-user");
        user.setEnabled(true);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "breadbite-user",
                null,
                java.util.Collections.emptyList());

        when(userService.findByUsername("breadbite-user"))
                .thenReturn(user);

        User result = currentCustomerService.resolve(authentication);

        assertSame(user, result);

        verify(userService).findByUsername("breadbite-user");

        verifyNoInteractions(externalIdentityService);
    }

    @Test
    void shouldResolveLinkedKeycloakCustomer() throws Exception {

        OidcUser oidcUser = mock(OidcUser.class);
        OidcIdToken idToken = mock(OidcIdToken.class);

        Authentication authentication = mock(Authentication.class);

        User user = new User();
        user.setUsername("linked-customer");
        user.setEnabled(true);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(oidcUser);

        when(oidcUser.getIdToken()).thenReturn(idToken);
        when(idToken.getIssuer()).thenReturn(new URL(ISSUER));
        when(oidcUser.getSubject()).thenReturn(SUBJECT);

        when(externalIdentityService.resolveUser(ISSUER, SUBJECT))
                .thenReturn(user);

        User result = currentCustomerService.resolve(authentication);

        assertSame(user, result);

        verify(externalIdentityService)
                .resolveUser(ISSUER, SUBJECT);

        verifyNoInteractions(userService);
    }

    @Test
    void shouldRejectMissingAuthentication() {

        assertThrows(
                AccessDeniedException.class,
                () -> currentCustomerService.resolve(null));

        verifyNoInteractions(userService, externalIdentityService);
    }

    @Test
    void shouldRejectDisabledLegacyCustomer() {

        User user = new User();
        user.setUsername("disabled-user");
        user.setEnabled(false);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "disabled-user",
                null,
                java.util.Collections.emptyList());

        when(userService.findByUsername("disabled-user"))
                .thenReturn(user);

        assertThrows(
                AccessDeniedException.class,
                () -> currentCustomerService.resolve(authentication));
    }

    @Test
    void shouldRejectUnlinkedKeycloakCustomer() throws Exception {

        OidcUser oidcUser = mock(OidcUser.class);
        OidcIdToken idToken = mock(OidcIdToken.class);

        Authentication authentication = mock(Authentication.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(oidcUser);

        when(oidcUser.getIdToken()).thenReturn(idToken);
        when(idToken.getIssuer()).thenReturn(new URL(ISSUER));
        when(oidcUser.getSubject()).thenReturn(SUBJECT);

        when(externalIdentityService.resolveUser(ISSUER, SUBJECT))
                .thenThrow(new AccessDeniedException(
                        "External identity is not linked"));

        assertThrows(
                AccessDeniedException.class,
                () -> currentCustomerService.resolve(authentication));

        verifyNoInteractions(userService);
    }

    @Test
    void shouldResolveLocalUsernameForKeycloakCustomer()
            throws Exception {

        OidcUser oidcUser = mock(OidcUser.class);
        OidcIdToken idToken = mock(OidcIdToken.class);

        Authentication authentication = mock(Authentication.class);

        User user = new User();
        user.setUsername("breadbite-local-user");
        user.setEnabled(true);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(oidcUser);

        when(oidcUser.getIdToken()).thenReturn(idToken);
        when(idToken.getIssuer()).thenReturn(new URL(ISSUER));
        when(oidcUser.getSubject()).thenReturn(SUBJECT);

        when(externalIdentityService.resolveUser(ISSUER, SUBJECT))
                .thenReturn(user);

        String username = currentCustomerService.resolveUsername(authentication);

        assertEquals("breadbite-local-user", username);
    }

    @Test
    void shouldRejectUnsupportedPrincipal() {

        Authentication authentication = mock(Authentication.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");

        assertThrows(
                AccessDeniedException.class,
                () -> currentCustomerService.resolve(authentication));

        verifyNoInteractions(userService, externalIdentityService);
    }
}
