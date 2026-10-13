
package com.breadbitt.repository;

import com.breadbitt.domain.ExternalIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExternalIdentityRepository
        extends JpaRepository<ExternalIdentity, Long> {

    Optional<ExternalIdentity> findByIssuerAndSubject(
            String issuer,
            String subject);

    boolean existsByIssuerAndSubject(
            String issuer,
            String subject);
}
