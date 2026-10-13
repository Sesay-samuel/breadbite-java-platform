
package com.breadbitt.repository;

import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.breadbitt.domain.User;
import com.breadbitt.domain.security.PasswordResetToken;

import jakarta.persistence.LockModeType;

public interface PasswordResetTokenRepository
		extends JpaRepository<PasswordResetToken, Long> {

	// The service will pass a SHA-256 hash,
	// not the raw token from the reset link.
	PasswordResetToken findByToken(String token);

	PasswordResetToken findByUser(User user);

	// Lock the token record while completing a reset.
	// Call this within a @Transactional service method.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT t
			FROM PasswordResetToken t
			WHERE t.token = :tokenHash
			""")
	Optional<PasswordResetToken> findByTokenForUpdate(
			@Param("tokenHash") String tokenHash);

	Stream<PasswordResetToken> findAllByExpiryDateLessThan(Date now);

	@Modifying
	@Query("""
			DELETE FROM PasswordResetToken t
			WHERE t.expiryDate <= :now
			""")
	void deleteAllExpiredSince(@Param("now") Date now);
}
