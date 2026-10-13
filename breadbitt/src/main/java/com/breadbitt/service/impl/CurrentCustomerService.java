
package com.breadbitt.service.impl;

import com.breadbitt.domain.User;
import com.breadbitt.service.UserService;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Service
public class CurrentCustomerService {

    private final UserService userService;
    private final ExternalIdentityService externalIdentityService;

    public CurrentCustomerService(
            UserService userService,
            ExternalIdentityService externalIdentityService) {

        this.userService = userService;
        this.externalIdentityService = externalIdentityService;
    }

    public User resolve(Principal principal) {

        if (!(principal instanceof Authentication authentication)
                || !authentication.isAuthenticated()) {

            throw new AccessDeniedException(
                    "Customer authentication is required");
        }

        Object authenticatedPrincipal = authentication.getPrincipal();

        // Resolve a verified Keycloak OIDC identity.
        if (authenticatedPrincipal instanceof OidcUser oidcUser) {

            if (oidcUser.getIdToken() == null
                    || oidcUser.getIdToken().getIssuer() == null
                    || oidcUser.getSubject() == null
                    || oidcUser.getSubject().isBlank()) {

                throw new AccessDeniedException(
                        "Invalid Keycloak identity");
            }

            String issuer = oidcUser.getIdToken().getIssuer().toString();

            String subject = oidcUser.getSubject();

            return externalIdentityService.resolveUser(
                    issuer, subject);
        }

        // Only accept traditional username/password authentication
        // for the local Breadbite customer lookup.
        if (!(authentication instanceof UsernamePasswordAuthenticationToken)) {

            throw new AccessDeniedException(
                    "Unsupported customer authentication");
        }

        String username;

        if (authenticatedPrincipal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();

        } else if (authenticatedPrincipal instanceof String name) {
            username = name;

        } else {
            throw new AccessDeniedException(
                    "Unsupported customer principal");
        }

        if (username == null || username.isBlank()) {
            throw new AccessDeniedException(
                    "Invalid Breadbite username");
        }

        User user = userService.findByUsername(username);

        if (user == null || !user.isEnabled()) {
            throw new AccessDeniedException(
                    "Breadbite account is unavailable");
        }

        return user;
    }

    public String resolveUsername(Principal principal) {
        return resolve(principal).getUsername();
    }
}
