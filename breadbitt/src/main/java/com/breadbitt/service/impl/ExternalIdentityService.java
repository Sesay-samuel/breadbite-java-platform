
package com.breadbitt.service.impl;

import com.breadbitt.domain.ExternalIdentity;
import com.breadbitt.domain.User;
import com.breadbitt.repository.ExternalIdentityRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExternalIdentityService {

    private final ExternalIdentityRepository externalIdentityRepository;

    public ExternalIdentityService(
            ExternalIdentityRepository externalIdentityRepository) {
        this.externalIdentityRepository = externalIdentityRepository;
    }

    @Transactional(readOnly = true)
    public User resolveUser(String issuer, String subject) {

        if (issuer == null || issuer.isBlank()
                || subject == null || subject.isBlank()) {
            throw new AccessDeniedException(
                    "Invalid external identity");
        }

        ExternalIdentity identity = externalIdentityRepository
                .findByIssuerAndSubject(issuer, subject)
                .orElseThrow(() -> new AccessDeniedException(
                        "External identity is not linked to a Breadbite account"));

        User user = identity.getUser();

        if (user == null || !user.isEnabled()) {
            throw new AccessDeniedException(
                    "Breadbite account is unavailable");
        }

        return user;
    }
}
