
package com.breadbitt.config;

import com.breadbitt.service.impl.ExternalIdentityService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class KeycloakLoginSuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final ExternalIdentityService externalIdentityService;

    public KeycloakLoginSuccessHandler(
            ExternalIdentityService externalIdentityService) {

        this.externalIdentityService = externalIdentityService;
        setDefaultTargetUrl("/");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        try {
            if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
                throw new AccessDeniedException(
                        "Expected an OpenID Connect identity");
            }

            String issuer = oidcUser.getIdToken()
                    .getIssuer().toString();

            String subject = oidcUser.getSubject();

            externalIdentityService.resolveUser(issuer, subject);

            super.onAuthenticationSuccess(
                    request, response, authentication);

        } catch (AccessDeniedException ex) {
            SecurityContextHolder.clearContext();

            if (request.getSession(false) != null) {
                request.getSession(false).invalidate();
            }

            getRedirectStrategy().sendRedirect(
                    request,
                    response,
                    "/login?keycloakUnlinked");
        }
    }
}
