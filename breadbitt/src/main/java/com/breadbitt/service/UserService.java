
package com.breadbitt.service;

import java.util.Set;

import com.breadbitt.domain.User;
import com.breadbitt.domain.UserBilling;
import com.breadbitt.domain.UserPayment;
import com.breadbitt.domain.UserShipping;
import com.breadbitt.domain.security.PasswordResetToken;
import com.breadbitt.domain.security.UserRole;

public interface UserService {

	// ========================================
	// PASSWORD RESET
	// ========================================

	PasswordResetToken getPasswordResetToken(
			final String token);

	void createPasswordResetTokenForUser(
			final User user,
			final String token);

	// Secure, single-use password reset.
	boolean resetPassword(
			String token,
			String newPassword);

	// ========================================
	// USER LOOKUP
	// ========================================

	User findByUsername(String username);

	User findByEmail(String email);

	User findById(Long id);

	// ========================================
	// CUSTOMER REGISTRATION
	// ========================================

	User createUser(
			User user,
			Set<UserRole> userRoles) throws Exception;

	// ========================================
	// SAVE USER
	// ========================================

	User save(User user);

	// ========================================
	// USER BILLING
	// ========================================

	void updateUserBilling(
			UserBilling userBilling,
			UserPayment userPayment,
			User user);

	// ========================================
	// USER SHIPPING
	// ========================================

	void updateUserShipping(
			UserShipping userShipping,
			User user);

	// ========================================
	// DEFAULT PAYMENT
	// ========================================

	void setUserDefaultPayment(
			Long userPaymentId,
			User user);

	// ========================================
	// DEFAULT SHIPPING
	// ========================================

	void setUserDefaultShipping(
			Long userShippingId,
			User user);
}
