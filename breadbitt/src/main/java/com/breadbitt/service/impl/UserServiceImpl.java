
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
import org.springframework.security.access.AccessDeniedException;
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

		if (token == null
				|| token.isBlank()
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

		user.setPassword(
				PASSWORD_ENCODER.encode(newPassword));

		userRepository.save(user);

		// Consume the token in the same transaction.
		passwordResetTokenRepository.delete(resetToken);

		LOG.info(
				"Customer password reset completed successfully");

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

		User existingUser = userRepository.findByUsername(
				user.getUsername());

		if (existingUser != null) {

			LOG.warn(
					"Registration rejected: username already exists");

			throw new IllegalArgumentException(
					"Username is already registered");
		}

		// Find the existing customer role.
		Role customerRole = roleRepository.findByName(CUSTOMER_ROLE);

		// Create the role if it does not exist.
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
	@Transactional
	public void updateUserBilling(
			UserBilling userBilling,
			UserPayment userPayment,
			User user) {

		if (userBilling == null
				|| userPayment == null
				|| user == null
				|| user.getId() == null) {

			throw new IllegalArgumentException(
					"Valid billing, payment and user are required");
		}

		User managedUser = userRepository.findById(user.getId())
				.orElseThrow(
						() -> new IllegalArgumentException(
								"User not found"));

		// Do not allow a new-payment operation to overwrite
		// another customer's existing payment record.
		if (userPayment.getId() != null) {
			throw new IllegalArgumentException(
					"Use a dedicated update operation to edit payment details");
		}

		userPayment.setId(null);
		userPayment.setUser(managedUser);
		userPayment.setUserBilling(userBilling);

		userBilling.setId(null);
		userBilling.setUserPayment(userPayment);

		// Preserve the existing default where one exists.
		List<UserPayment> existingPayments = userPaymentRepository.findByUser_Id(
				managedUser.getId());

		boolean hasDefault = existingPayments.stream()
				.anyMatch(UserPayment::isDefaultPayment);

		userPayment.setDefaultPayment(!hasDefault);

		// Persist through the existing user mapping.
		// This collection is accessed inside a transaction.
		managedUser.getUserPaymentList().add(userPayment);

		userRepository.save(managedUser);

		LOG.info(
				"Customer billing and payment method saved");
	}

	// ========================================
	// USER SHIPPING
	// ========================================

	@Override
	@Transactional
	public void updateUserShipping(
			UserShipping userShipping,
			User user) {

		if (userShipping == null
				|| user == null
				|| user.getId() == null) {

			throw new IllegalArgumentException(
					"Valid user and shipping address are required");
		}

		Long userId = user.getId();

		// Load a managed user within the transaction.
		User managedUser = userRepository.findById(userId)
				.orElseThrow(
						() -> new IllegalArgumentException(
								"User not found"));

		// This method creates a new shipping address.
		// Existing addresses require a separate,
		// ownership-checked update operation.
		if (userShipping.getId() != null) {
			throw new IllegalArgumentException(
					"Use the dedicated update operation to edit an address");
		}

		// The server sets ownership.
		userShipping.setId(null);
		userShipping.setUser(managedUser);

		List<UserShipping> existingAddresses = userShippingRepository.findByUser_Id(userId);

		boolean hasDefault = existingAddresses.stream()
				.anyMatch(UserShipping::isUserShippingDefault);

		// The first address becomes the default.
		userShipping.setUserShippingDefault(!hasDefault);

		// Save directly without touching the detached
		// user's lazy-loaded shipping collection.
		userShippingRepository.save(userShipping);

		LOG.info(
				"Customer shipping address saved successfully");
	}

	// ========================================
	// SECURE DEFAULT PAYMENT
	// ========================================

	@Override
	@Transactional
	public void setUserDefaultPayment(
			Long userPaymentId,
			User user) {

		if (user == null
				|| user.getId() == null
				|| userPaymentId == null) {

			throw new IllegalArgumentException(
					"Valid user and payment ID are required");
		}

		// Only retrieve payment methods owned by this user.
		List<UserPayment> userPayments = userPaymentRepository.findByUser_Id(
				user.getId());

		// Reject IDs that do not belong to the user.
		boolean ownsPayment = userPayments.stream()
				.anyMatch(
						payment -> userPaymentId.equals(
								payment.getId()));

		if (!ownsPayment) {
			throw new AccessDeniedException(
					"Payment method does not belong to this user");
		}

		// Change defaults only within this user's records.
		for (UserPayment payment : userPayments) {

			payment.setDefaultPayment(
					userPaymentId.equals(payment.getId()));
		}

		userPaymentRepository.saveAll(userPayments);

		LOG.info(
				"Customer default payment method updated");
	}

	// ========================================
	// SECURE DEFAULT SHIPPING
	// ========================================

	@Override
	@Transactional
	public void setUserDefaultShipping(
			Long userShippingId,
			User user) {

		if (user == null
				|| user.getId() == null
				|| userShippingId == null) {

			throw new IllegalArgumentException(
					"Valid user and shipping ID are required");
		}

		// Only retrieve addresses owned by this user.
		List<UserShipping> userShippingList = userShippingRepository.findByUser_Id(
				user.getId());

		// Reject IDs that do not belong to the user.
		boolean ownsShipping = userShippingList.stream()
				.anyMatch(
						shipping -> userShippingId.equals(
								shipping.getId()));

		if (!ownsShipping) {
			throw new AccessDeniedException(
					"Shipping address does not belong to this user");
		}

		// Change defaults only within this user's records.
		for (UserShipping shipping : userShippingList) {

			shipping.setUserShippingDefault(
					userShippingId.equals(
							shipping.getId()));
		}

		userShippingRepository.saveAll(userShippingList);

		LOG.info(
				"Customer default shipping address updated");
	}
}
