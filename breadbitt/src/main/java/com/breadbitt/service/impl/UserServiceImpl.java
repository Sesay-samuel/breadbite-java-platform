
package com.breadbitt.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
	// PASSWORD RESET
	// ========================================

	@Override
	public PasswordResetToken getPasswordResetToken(
			final String token) {

		return passwordResetTokenRepository.findByToken(token);
	}

	@Override
	public void createPasswordResetTokenForUser(
			final User user,
			final String token) {

		final PasswordResetToken myToken = new PasswordResetToken(token, user);

		passwordResetTokenRepository.save(myToken);
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

		if (user == null ||
				user.getUsername() == null ||
				user.getUsername().isBlank()) {

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

		// Create the customer role only if it does not exist.
		if (customerRole == null) {

			customerRole = new Role();
			customerRole.setRoleId(1);
			customerRole.setName(CUSTOMER_ROLE);

			customerRole = roleRepository.save(customerRole);
		}

		// Never trust caller-supplied roles during
		// public customer registration.
		UserRole customerUserRole = new UserRole(user, customerRole);

		user.getUserRoles().clear();
		user.getUserRoles().add(customerUserRole);

		// Create a shopping cart for the new user.
		ShoppingCart shoppingCart = new ShoppingCart();
		shoppingCart.setUser(user);

		user.setShoppingCart(shoppingCart);

		// Initialize payment and shipping collections.
		user.setUserShippingList(
				new ArrayList<UserShipping>());

		user.setUserPaymentList(
				new ArrayList<UserPayment>());

		// Persist the customer and associated entities.
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
