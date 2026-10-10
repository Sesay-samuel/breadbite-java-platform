
package com.breadbitt.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.breadbitt.domain.ShoppingCart;
import com.breadbitt.domain.User;
import com.breadbitt.domain.UserBilling;
import com.breadbitt.domain.UserPayment;
import com.breadbitt.domain.UserShipping;

import com.breadbitt.domain.security.PasswordResetToken;
import com.breadbitt.domain.security.Role;
import com.breadbitt.domain.security.UserRole;

import com.breadbitt.repository.PasswordResetTokenRepository;
import com.breadbitt.repository.RoleRepository;
import com.breadbitt.repository.UserPaymentRepository;
import com.breadbitt.repository.UserRepository;
import com.breadbitt.repository.UserShippingRepository;

import com.breadbitt.service.UserService;

@Service
public class UserServiceImpl implements UserService {

	private static final Logger LOG = LoggerFactory.getLogger(UserServiceImpl.class);

	private static final String CUSTOMER_ROLE = "ROLE_USER";

	private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(12);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private UserPaymentRepository userPaymentRepository;

	@Autowired
	private UserShippingRepository userShippingRepository;

	@Autowired
	private PasswordResetTokenRepository passwordResetTokenRepository;

	// ========================================
	// PASSWORD RESET TOKEN LOOKUP
	// ========================================

	@Override
	@Transactional(readOnly = true)
	public PasswordResetToken getPasswordResetToken(
			final String token) {

		if (token == null || token.isBlank()) {
			return null;
		}

		String tokenHash = hashResetToken(token);

		PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(tokenHash);

		if (resetToken == null || resetToken.isExpired()) {
			return null;
		}

		return resetToken;
	}

	// ========================================
	// CREATE OR REPLACE RESET TOKEN
	// ========================================

	@Override
	@Transactional
	public void createPasswordResetTokenForUser(
			final User user,
			final String token) {

		if (user == null || token == null || token.isBlank()) {
			throw new IllegalArgumentException(
					"User and reset token are required");
		}

		String tokenHash = hashResetToken(token);

		PasswordResetToken existingToken = passwordResetTokenRepository.findByUser(user);

		if (existingToken != null) {

			existingToken.updateToken(tokenHash);

			passwordResetTokenRepository.save(existingToken);

		} else {

			PasswordResetToken newToken = new PasswordResetToken(tokenHash, user);

			passwordResetTokenRepository.save(newToken);
		}
	}

	// ========================================
	// SECURE SINGLE-USE PASSWORD RESET
	// ========================================

	@Override
	@Transactional
	public boolean resetPassword(
			final String token,
			final String newPassword) {

		if (token == null || token.isBlank()
				|| newPassword == null
				|| newPassword.length() < 12
				|| newPassword.length() > 128) {

			return false;
		}

		String tokenHash = hashResetToken(token);

		Optional<PasswordResetToken> tokenOptional = passwordResetTokenRepository
				.findByTokenForUpdate(tokenHash);

		if (tokenOptional.isEmpty()) {
			return false;
		}

		PasswordResetToken resetToken = tokenOptional.get();

		if (resetToken.isExpired()) {
			return false;
		}

		User user = resetToken.getUser();

		if (user == null) {
			return false;
		}

		// Hash the new password using BCrypt.
		user.setPassword(
				PASSWORD_ENCODER.encode(newPassword));

		userRepository.save(user);

		// Consume the reset token in the same transaction.
		passwordResetTokenRepository.delete(resetToken);

		LOG.info("Customer password reset completed successfully");

		return true;
	}

	// ========================================
	// SHA-256 TOKEN HASHING
	// ========================================

	private String hashResetToken(final String token) {

		try {

			MessageDigest digest = MessageDigest.getInstance("SHA-256");

			byte[] hash = digest.digest(
					token.getBytes(StandardCharsets.UTF_8));

			return HexFormat.of().formatHex(hash);

		} catch (NoSuchAlgorithmException exception) {

			throw new IllegalStateException(
					"SHA-256 is unavailable", exception);
		}
	}

	// ========================================
	// USER LOOKUP
	// ========================================

	@Override
	public User findByUsername(String username) {

		return userRepository.findByUsername(username);
	}

	@Override
	public User findById(Long id) {

		Optional<User> optionalUser = userRepository.findById(id);

		return optionalUser.orElse(null);
	}

	@Override
	public User findByEmail(String email) {

		return userRepository.findByEmail(email);
	}

	// ========================================
	// SECURE CUSTOMER REGISTRATION
	// ========================================

	@Override
	@Transactional
	public User createUser(
			User user,
			Set<UserRole> userRoles) {

		if (user == null
				|| user.getUsername() == null
				|| user.getUsername().isBlank()) {

			throw new IllegalArgumentException(
					"A valid username is required");
		}

		User existingUser = userRepository.findByUsername(user.getUsername());

		if (existingUser != null) {

			LOG.warn(
					"Registration rejected: username already exists");

			throw new IllegalArgumentException(
					"Username is already registered");
		}

		// Find the existing customer role.
		Role customerRole = roleRepository.findByName(CUSTOMER_ROLE);

		// Create the customer role if it does not exist.
		if (customerRole == null) {

			customerRole = new Role();

			customerRole.setRoleId(1);
			customerRole.setName(CUSTOMER_ROLE);

			customerRole = roleRepository.save(customerRole);
		}

		// Never accept caller-supplied administrative roles.
		UserRole customerUserRole = new UserRole(user, customerRole);

		user.getUserRoles().clear();
		user.getUserRoles().add(customerUserRole);

		// Create a shopping cart.
		ShoppingCart shoppingCart = new ShoppingCart();
		shoppingCart.setUser(user);

		user.setShoppingCart(shoppingCart);

		// Initialize shipping and payment collections.
		user.setUserShippingList(
				new ArrayList<UserShipping>());

		user.setUserPaymentList(
				new ArrayList<UserPayment>());

		User savedUser = userRepository.save(user);

		LOG.info(
				"New customer registered successfully");

		return savedUser;
	}

	// ========================================
	// SAVE USER
	// ========================================

	@Override
	public User save(User user) {

		return userRepository.save(user);
	}

	// ========================================
	// USER BILLING
	// ========================================

	@Override
	public void updateUserBilling(
			UserBilling userBilling,
			UserPayment userPayment,
			User user) {

		userPayment.setUser(user);

		userPayment.setUserBilling(userBilling);

		userPayment.setDefaultPayment(true);

		userBilling.setUserPayment(userPayment);

		user.getUserPaymentList().add(userPayment);

		save(user);
	}

	// ========================================
	// USER SHIPPING
	// ========================================

	@Override
	public void updateUserShipping(
			UserShipping userShipping,
			User user) {

		userShipping.setUser(user);

		userShipping.setUserShippingDefault(true);

		user.getUserShippingList().add(userShipping);

		save(user);
	}

	// ========================================
	// DEFAULT PAYMENT
	// ========================================

	@Override
	public void setUserDefaultPayment(
			Long userPaymentId,
			User user) {

		List<UserPayment> userPaymentList = (List<UserPayment>) userPaymentRepository.findAll();

		for (UserPayment userPayment : userPaymentList) {

			if (userPayment.getId().equals(userPaymentId)) {

				userPayment.setDefaultPayment(true);

			} else {

				userPayment.setDefaultPayment(false);
			}

			userPaymentRepository.save(userPayment);
		}
	}

	// ========================================
	// DEFAULT SHIPPING
	// ========================================

	@Override
	public void setUserDefaultShipping(
			Long userShippingId,
			User user) {

		List<UserShipping> userShippingList = (List<UserShipping>) userShippingRepository.findAll();

		for (UserShipping userShipping : userShippingList) {

			if (userShipping.getId().equals(userShippingId)) {

				userShipping.setUserShippingDefault(true);

			} else {

				userShipping.setUserShippingDefault(false);
			}

			userShippingRepository.save(userShipping);
		}
	}
}
