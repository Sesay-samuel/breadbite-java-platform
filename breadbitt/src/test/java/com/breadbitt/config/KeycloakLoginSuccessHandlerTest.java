
package com.breadbitt.config;

import com.breadbitt.domain.User;
import com.breadbitt.service.impl.ExternalIdentityService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.net.URL;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class KeycloakLoginSuccessHandlerTest {

    private static final String ISSUER = "http://localhost:8180/realms/breadbite";

    private static final String SUBJECT = "keycloak-test-subject-123";

    private ExternalIdentityService identityService;
    private KeycloakLoginSuccessHandler handler;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Authentication authentication;
    private OidcUser oidcUser;
    private OidcIdToken idToken;

    @BeforeEach
    void setUp() throws Exception {

        identityService = mock(ExternalIdentityService.class);
        handler = new KeycloakLoginSuccessHandler(identityService);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        authentication = mock(Authentication.class);
        oidcUser = mock(OidcUser.class);
        idToken = mock(OidcIdToken.class);

        // Simulate servlet redirect behaviour.
        when(request.getContextPath()).thenReturn("");

        when(response.encodeRedirectURL(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Mock the Keycloak identity.
        when(authentication.getPrincipal()).thenReturn(oidcUser);

        when(oidcUser.getIdToken()).thenReturn(idToken);

        when(idToken.getIssuer()).thenReturn(new URL(ISSUER));

        when(oidcUser.getSubject()).thenReturn(SUBJECT);
    }

    @Test
    void shouldAllowLinkedEnabledUser() throws Exception {

        User user = new User();
        user.setEnabled(true);

        when(identityService.resolveUser(ISSUER, SUBJECT))
                .thenReturn(user);

        handler.onAuthenticationSuccess(
                request,
                response,
                authentication);

        verify(identityService)
                .resolveUser(ISSUER, SUBJECT);

        verify(response).sendRedirect("/");
    }

    @Test
    void shouldRejectUnlinkedIdentity() throws Exception {

        when(identityService.resolveUser(ISSUER, SUBJECT))
                .thenThrow(new AccessDeniedException(
                        "External identity is not linked"));

        handler.onAuthenticationSuccess(
                request,
                response,
                authentication);

        verify(identityService)
                .resolveUser(ISSUER, SUBJECT);

        verify(response)
                .sendRedirect("/login?keycloakUnlinked");
    }

    @Test
    void shouldRejectNonOidcPrincipal() throws Exception {

        when(authentication.getPrincipal())
                .thenReturn("not-an-oidc-user");

        handler.onAuthenticationSuccess(
                request,
                response,
                authentication);

        verify(response)
                .sendRedirect("/login?keycloakUnlinked");

        verifyNoInteractions(identityService);
    }

    @Test
    void shouldInvalidateSessionForUnlinkedIdentity()
            throws Exception {

        HttpSession session = mock(HttpSession.class);

        when(request.getSession(false))
                .thenReturn(session);

        when(identityService.resolveUser(ISSUER, SUBJECT))
                .thenThrow(new AccessDeniedException(
                        "External identity is not linked"));

        handler.onAuthenticationSuccess(
                request,
                response,
                authentication);

        verify(session).invalidate();

        verify(response)
                .sendRedirect("/login?keycloakUnlinked");
    }

    @Test
    void shouldRejectDisabledBreadbiteUser() throws Exception {

        when(identityService.resolveUser(ISSUER, SUBJECT))
                .thenThrow(new AccessDeniedException(
                        "Breadbite account is unavailable"));

        handler.onAuthenticationSuccess(
                request,
                response,
                authentication);

        verify(identityService)
                .resolveUser(ISSUER, SUBJECT);

        verify(response)
                .sendRedirect("/login?keycloakUnlinked");
    }
}
