package com.breadbitt.controller;

import java.security.Principal;

import java.util.Arrays;

import java.util.Collections;

import java.util.HashSet;

import java.util.List;

import java.util.Locale;

import java.util.Optional;

import java.util.Set;

import java.security.SecureRandom;
import java.util.Base64;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.mail.SimpleMailMessage;

import org.springframework.mail.javamail.JavaMailSender;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;

import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.ModelAttribute;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestMethod;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.breadbitt.domain.Bread;

import com.breadbitt.domain.CartItem;

import com.breadbitt.domain.Order;

import com.breadbitt.domain.User;

import com.breadbitt.domain.UserBilling;

import com.breadbitt.domain.UserPayment;

import com.breadbitt.domain.UserShipping;

import com.breadbitt.domain.security.PasswordResetToken;

import com.breadbitt.domain.security.Role;

import com.breadbitt.domain.security.UserRole;

import com.breadbitt.service.BreadService;

import com.breadbitt.service.CartItemService;

import com.breadbitt.service.OrderService;

import com.breadbitt.service.UserPaymentService;

import com.breadbitt.service.UserService;

import com.breadbitt.service.UserShippingService;

import com.breadbitt.service.impl.UserSecurityService;

import com.breadbitt.utility.MailConstructor;

import com.breadbitt.utility.SecurityUtility;

import com.breadbitt.utility.USConstants;

@Controller

public class HomeController {

	@Value("${app.base-url}")
	private String applicationBaseUrl;

	private static final SecureRandom RESET_RANDOM = new SecureRandom();

	private String generateResetToken() {
		byte[] bytes = new byte[32];
		RESET_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	@Autowired

	private JavaMailSender mailSender;

	@Autowired

	private MailConstructor mailConstructor;

	@Autowired

	private UserService userService;

	@Autowired

	private UserSecurityService userSecurityService;

	@Autowired

	private BreadService breadService;

	@Autowired

	private UserPaymentService userPaymentService;

	@Autowired

	private UserShippingService userShippingService;

	@Autowired

	private CartItemService cartItemService;

	@Autowired

	private OrderService orderService;

	@RequestMapping("/")

	public String index() {

		return "index";

	}

	@RequestMapping("/login")

	public String login(Model model) {

		model.addAttribute("classActiveLogin", true);

		return "myAccount";

	}

	@RequestMapping("/breadShelf")

	public String breadShelf(Model model) {

		List<Bread> breadList = breadService.findAll();

		model.addAttribute("breadList", breadList);

		return "breadShelf";

	}

	@RequestMapping("/breadDetail")

	public String breadDetail(

			@RequestParam("id") Long id, Model model, Principal principal) {

		if (principal != null) {

			String username = principal.getName();

			User user = userService.findByUsername(username);

			model.addAttribute("user", user);

		}

		Optional<Bread> breadOptional = breadService.findById(id);

		if (breadOptional.isPresent()) {

			model.addAttribute("bread", breadOptional.get());

		} else {

			// Handle the case where bread is not found

			return "redirect:/error"; // Redirect to an error page or handle accordingly

		}

		List<Integer> qtyList = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

		model.addAttribute("qtyList", qtyList);

		model.addAttribute("qty", 1);

		return "breadDetail";

	}

	@PostMapping("/forgetPassword")
	public String forgetPassword(@RequestParam("email") String email, Model model) {
		model.addAttribute("classActiveForgetPassword", true);
		if (email != null && !email.isBlank()) {
			User user = userService.findByEmail(email.trim());
			if (user != null) {
				String token = generateResetToken();
				userService.createPasswordResetTokenForUser(user, token);
				SimpleMailMessage message = mailConstructor.constructPasswordSetupEmail(
						applicationBaseUrl, token, user, false);
				mailSender.send(message);
			}
		}
		// Generic response avoids disclosing registered email addresses.
		model.addAttribute("forgetPasswordEmailSent", true);
		return "myAccount";
	}

	@RequestMapping("/myProfile")

	public String myProfile(Model model, Principal principal) {

		User user = userService.findByUsername(principal.getName());

		model.addAttribute("user", user);

		model.addAttribute("userPaymentList", user.getUserPaymentList());

		model.addAttribute("userShippingList", user.getUserShippingList());

		model.addAttribute("orderList", user.getOrderList());

		// Initialize a new UserPayment object and add it to the model

		UserPayment userPayment = new UserPayment();

		model.addAttribute("userPayment", userPayment);

		UserShipping userShipping = new UserShipping();

		model.addAttribute("userShipping", userShipping);

		model.addAttribute("listOfCreditCards", true);

		model.addAttribute("listOfShippingAddresses", true);

		List<String> stateList = USConstants.listOfUSStatesCode;

		Collections.sort(stateList);

		model.addAttribute("stateList", stateList);

		model.addAttribute("classActiveEdit", true);

		return "myProfile";

	}

	@RequestMapping("/listOfCreditCards")

	public String listOfCreditCards(

			Model model, Principal principal, HttpServletRequest request

	) {

		User user = userService.findByUsername(principal.getName());

		model.addAttribute("user", user);

		model.addAttribute("userPaymentList", user.getUserPaymentList());

		model.addAttribute("userShippingList", user.getUserShippingList());

		model.addAttribute("orderList", user.getOrderList());

		model.addAttribute("listOfCreditCards", true);

		model.addAttribute("classActiveBilling", true);

		model.addAttribute("listOfShippingAddresses", true);

		return "myProfile";

	}

	@RequestMapping("/listOfShippingAddresses")

	public String listOfShippingAddresses(

			Model model, Principal principal, HttpServletRequest request

	) {

		User user = userService.findByUsername(principal.getName());

		model.addAttribute("user", user);

		model.addAttribute("userPaymentList", user.getUserPaymentList());

		model.addAttribute("userShippingList", user.getUserShippingList());

		model.addAttribute("orderList", user.getOrderList());

		model.addAttribute("listOfCreditCards", true);

		model.addAttribute("classActiveShipping", true);

		model.addAttribute("listOfShippingAddresses", true);

		return "myProfile";

	}

	@RequestMapping("/addNewCreditCard")

	public String addNewCreditCard(

			Model model, Principal principal

	) {

		User user = userService.findByUsername(principal.getName());

		model.addAttribute("user", user);

		model.addAttribute("addNewCreditCard", true);

		model.addAttribute("classActiveBilling", true);

		model.addAttribute("listOfShippingAddresses", true);

		UserBilling userBilling = new UserBilling();

		UserPayment userPayment = new UserPayment();

		model.addAttribute("userBilling", userBilling);

		model.addAttribute("userPayment", userPayment);

		List<String> stateList = USConstants.listOfUSStatesCode;

		Collections.sort(stateList);

		model.addAttribute("stateList", stateList);

		model.addAttribute("userPaymentList", user.getUserPaymentList());

		model.addAttribute("userShippingList", user.getUserShippingList());

		model.addAttribute("orderList", user.getOrderList());

		return "myProfile";

	}

	@RequestMapping("/addNewShippingAddress")

	public String addNewShippingAddress(

			Model model, Principal principal

	) {

		User user = userService.findByUsername(principal.getName());

		model.addAttribute("user", user);

		model.addAttribute("addNewShippingAddress", true);

		model.addAttribute("classActiveShipping", true);

		model.addAttribute("listOfCreditCards", true);

		UserShipping userShipping = new UserShipping();

		model.addAttribute("userShipping", userShipping);

		List<String> stateList = USConstants.listOfUSStatesCode;

		Collections.sort(stateList);

		model.addAttribute("stateList", stateList);

		model.addAttribute("userPaymentList", user.getUserPaymentList());

		model.addAttribute("userShippingList", user.getUserShippingList());

		model.addAttribute("orderList", user.getOrderList());

		return "myProfile";

	}

	@RequestMapping(value = "/addNewCreditCard", method = RequestMethod.POST)

	public String addNewCreditCard(

			@ModelAttribute("userPayment") UserPayment userPayment,

			@ModelAttribute("userBilling") UserBilling userBilling,

			Principal principal, Model model

	) {

		User user = userService.findByUsername(principal.getName());

		userService.updateUserBilling(userBilling, userPayment, user);

		model.addAttribute("user", user);

		model.addAttribute("userPaymentList", user.getUserPaymentList());

		model.addAttribute("userShippingList", user.getUserShippingList());

		model.addAttribute("listOfCreditCards", true);

		model.addAttribute("classActiveBilling", true);

		model.addAttribute("listOfShippingAddresses", true);

		model.addAttribute("orderList", user.getOrderList());

		return "myProfile";

	}

	@RequestMapping(value = "/addNewShippingAddress", method = RequestMethod.POST)

	public String addNewShippingAddressPost(

			@ModelAttribute("userShipping") UserShipping userShipping,

			Principal principal, Model model

	) {

		User user = userService.findByUsername(principal.getName());

		userService.updateUserShipping(userShipping, user);

		model.addAttribute("user", user);

		model.addAttribute("userPaymentList", user.getUserPaymentList());

		model.addAttribute("userShippingList", user.getUserShippingList());

		model.addAttribute("listOfShippingAddresses", true);

		model.addAttribute("classActiveShipping", true);

		model.addAttribute("listOfCreditCards", true);

		model.addAttribute("orderList", user.getOrderList());

		return "myProfile";

	}

	@RequestMapping("/updateCreditCard")

	public String updateCreditCard(

			@ModelAttribute("id") Long creditCardId, Principal principal, Model model

	) {

		User user = userService.findByUsername(principal.getName());

		UserPayment userPayment = userPaymentService.findById(creditCardId);

		if (user.getId() != userPayment.getUser().getId()) {

			return "badRequestPage";

		} else {

			model.addAttribute("user", user);

			UserBilling userBilling = userPayment.getUserBilling();

			model.addAttribute("userPayment", userPayment);

			model.addAttribute("userBilling", userBilling);

			List<String> stateList = USConstants.listOfUSStatesCode;

			Collections.sort(stateList);

			model.addAttribute("stateList", stateList);

			model.addAttribute("addNewCreditCard", true);

			model.addAttribute("classActiveBilling", true);

			model.addAttribute("listOfShippingAddresses", true);

			model.addAttribute("userPaymentList", user.getUserPaymentList());

			model.addAttribute("userShippingList", user.getUserShippingList());

			model.addAttribute("orderList", user.getOrderList());

			return "myProfile";

		}

	}

	@RequestMapping("/updateUserShipping")

	public String updateUserShipping(

			@ModelAttribute("id") Long shippingAddressId, Principal principal, Model model

	) {

		User user = userService.findByUsername(principal.getName());

		UserShipping userShipping = userShippingService.findById(shippingAddressId);

		if (user.getId() != userShipping.getUser().getId()) {

			return "badRequestPage";

		} else {

			model.addAttribute("user", user);

			model.addAttribute("userShipping", userShipping);

			List<String> stateList = USConstants.listOfUSStatesCode;

			Collections.sort(stateList);

			model.addAttribute("stateList", stateList);

			model.addAttribute("addNewShippingAddress", true);

			model.addAttribute("classActiveShipping", true);

			model.addAttribute("listOfCreditCards", true);

			model.addAttribute("userPaymentList", user.getUserPaymentList());

			model.addAttribute("userShippingList", user.getUserShippingList());

			model.addAttribute("orderList", user.getOrderList());

			return "myProfile";

		}

	}

	@RequestMapping(value = "/setDefaultPayment", method = RequestMethod.POST)

	public String setDefaultPayment(

			@ModelAttribute("defaultUserPaymentId") Long defaultPaymentId, Principal principal, Model model

	) {

		User user = userService.findByUsername(principal.getName());

		userService.setUserDefaultPayment(defaultPaymentId, user);

		model.addAttribute("user", user);

		model.addAttribute("listOfCreditCards", true);

		model.addAttribute("classActiveBilling", true);

		model.addAttribute("listOfShippingAddresses", true);

		model.addAttribute("userPaymentList", user.getUserPaymentList());

		model.addAttribute("userShippingList", user.getUserShippingList());

		model.addAttribute("orderList", user.getOrderList());

		return "myProfile";

	}

	@RequestMapping(value = "/setDefaultShippingAddress", method = RequestMethod.POST)

	public String setDefaultShippingAddress(

			@ModelAttribute("defaultShippingAddressId") Long defaultShippingId, Principal principal, Model model

	) {

		User user = userService.findByUsername(principal.getName());

		userService.setUserDefaultShipping(defaultShippingId, user);

		model.addAttribute("user", user);

		model.addAttribute("listOfCreditCards", true);

		model.addAttribute("classActiveShipping", true);

		model.addAttribute("listOfShippingAddresses", true);

		model.addAttribute("userPaymentList", user.getUserPaymentList());

		model.addAttribute("userShippingList", user.getUserShippingList());

		model.addAttribute("orderList", user.getOrderList());

		return "myProfile";

	}

	@RequestMapping("/removeCreditCard")

	public String removeCreditCard(

			@ModelAttribute("id") Long creditCardId, Principal principal, Model model

	) {

		User user = userService.findByUsername(principal.getName());

		UserPayment userPayment = userPaymentService.findById(creditCardId);

		if (user.getId() != userPayment.getUser().getId()) {

			return "badRequestPage";

		} else {

			model.addAttribute("user", user);

			userPaymentService.removeById(creditCardId);

			model.addAttribute("listOfCreditCards", true);

			model.addAttribute("classActiveBilling", true);

			model.addAttribute("listOfShippingAddresses", true);

			model.addAttribute("userPaymentList", user.getUserPaymentList());

			model.addAttribute("userShippingList", user.getUserShippingList());

			model.addAttribute("orderList", user.getOrderList());

			return "myProfile";

		}

	}

	@RequestMapping("/removeUserShipping")

	public String removeUserShipping(

			@ModelAttribute("id") Long userShippingId, Principal principal, Model model

	) {

		User user = userService.findByUsername(principal.getName());

		UserShipping userShipping = userShippingService.findById(userShippingId);

		if (user.getId() != userShipping.getUser().getId()) {

			return "badRequestPage";

		} else {

			model.addAttribute("user", user);

			userShippingService.removeById(userShippingId);

			model.addAttribute("listOfShippingAddresses", true);

			model.addAttribute("classActiveShipping", true);

			model.addAttribute("userPaymentList", user.getUserPaymentList());

			model.addAttribute("userShippingList", user.getUserShippingList());

			model.addAttribute("orderList", user.getOrderList());

			return "myProfile";

		}

	}

	@PostMapping("/newUser")
	public String newUserPost(
			HttpServletRequest request,
			@ModelAttribute("email") String userEmail,
			@ModelAttribute("username") String username,
			Model model) throws Exception {

		model.addAttribute("classActiveNewAccount", true);
		model.addAttribute("email", userEmail);
		model.addAttribute("username", username);

		if (username == null || username.isBlank() || userEmail == null || userEmail.isBlank()) {
			model.addAttribute("registrationError", true);
			return "myAccount";
		}
		username = username.trim();
		userEmail = userEmail.trim();

		if (userService.findByUsername(username) != null) {
			model.addAttribute("usernameExists", true);
			return "myAccount";
		}
		if (userService.findByEmail(userEmail) != null) {
			model.addAttribute("emailExists", true);
			return "myAccount";
		}

		User user = new User();
		user.setUsername(username);
		user.setEmail(userEmail);

		// Account starts with an unknown, strong random password.
		// The customer sets their own password using the emailed link.
		user.setPassword(SecurityUtility.passwordEncoder().encode(generateResetToken()));

		Role role = new Role();
		role.setRoleId(1);
		role.setName("ROLE_USER");
		Set<UserRole> userRoles = new HashSet<>();
		userRoles.add(new UserRole(user, role));
		user = userService.createUser(user, userRoles);

		String token = generateResetToken();
		userService.createPasswordResetTokenForUser(user, token);
		SimpleMailMessage email = mailConstructor.constructPasswordSetupEmail(
				applicationBaseUrl, token, user, true);
		mailSender.send(email);

		model.addAttribute("emailSent", true);
		return "myAccount";
	}

	@GetMapping("/newUser")
	public String newUser(@RequestParam("token") String token) {
		// Legacy links no longer authenticate the visitor.
		return "redirect:/resetPassword?token=" + token;
	}

	@GetMapping("/resetPassword")
	public String showResetPassword(@RequestParam("token") String token, Model model) {
		PasswordResetToken resetToken = userService.getPasswordResetToken(token);
		if (resetToken == null) {
			model.addAttribute("invalidToken", true);
		} else {
			model.addAttribute("token", token);
		}
		return "resetPassword";
	}

	@PostMapping("/resetPassword")
	public String processResetPassword(
			@RequestParam("token") String token,
			@RequestParam("newPassword") String newPassword,
			@RequestParam("confirmPassword") String confirmPassword,
			Model model) {
		if (newPassword == null || !newPassword.equals(confirmPassword)) {
			model.addAttribute("passwordMismatch", true);
			model.addAttribute("token", token);
			return "resetPassword";
		}
		if (newPassword.length() < 12 || newPassword.length() > 128) {
			model.addAttribute("passwordLengthInvalid", true);
			model.addAttribute("token", token);
			return "resetPassword";
		}
		if (!userService.resetPassword(token, newPassword)) {
			model.addAttribute("invalidToken", true);
			return "resetPassword";
		}
		return "redirect:/login?passwordReset";
	}

	@PostMapping("/updateUserInfo")
	public String updateUserInfo(
			@ModelAttribute("user") User submittedUser,
			@ModelAttribute("newPassword") String newPassword,
			Principal principal,
			Model model) {

		if (principal == null) {
			return "redirect:/login";
		}
		User currentUser = userService.findByUsername(principal.getName());
		if (currentUser == null) {
			return "redirect:/login";
		}

		// Ignore the submitted user ID: it is not an authorization credential.
		if (submittedUser.getEmail() == null || submittedUser.getEmail().isBlank()
				|| submittedUser.getUsername() == null || submittedUser.getUsername().isBlank()) {
			model.addAttribute("profileValidationError", true);
			return populateProfile(model, currentUser);
		}

		User emailOwner = userService.findByEmail(submittedUser.getEmail());
		if (emailOwner != null && !emailOwner.getId().equals(currentUser.getId())) {
			model.addAttribute("emailExists", true);
			return populateProfile(model, currentUser);
		}
		User usernameOwner = userService.findByUsername(submittedUser.getUsername());
		if (usernameOwner != null && !usernameOwner.getId().equals(currentUser.getId())) {
			model.addAttribute("usernameExists", true);
			return populateProfile(model, currentUser);
		}

		if (newPassword != null && !newPassword.isBlank()) {
			if (newPassword.length() < 12 || newPassword.length() > 128) {
				model.addAttribute("passwordLengthInvalid", true);
				return populateProfile(model, currentUser);
			}
			BCryptPasswordEncoder encoder = SecurityUtility.passwordEncoder();
			if (submittedUser.getPassword() == null
					|| !encoder.matches(submittedUser.getPassword(), currentUser.getPassword())) {
				model.addAttribute("incorrectPassword", true);
				return populateProfile(model, currentUser);
			}
			currentUser.setPassword(encoder.encode(newPassword));
		}

		currentUser.setFirstName(submittedUser.getFirstName());
		currentUser.setLastName(submittedUser.getLastName());
		currentUser.setUsername(submittedUser.getUsername());
		currentUser.setEmail(submittedUser.getEmail());
		userService.save(currentUser);

		// Re-authenticate only after validating the existing login session.
		UserDetails details = userSecurityService.loadUserByUsername(currentUser.getUsername());
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				details, null, details.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);

		model.addAttribute("updateSuccess", true);
		return populateProfile(model, currentUser);
	}

	private String populateProfile(Model model, User user) {
		model.addAttribute("user", user);
		model.addAttribute("userPaymentList", user.getUserPaymentList());
		model.addAttribute("userShippingList", user.getUserShippingList());
		model.addAttribute("orderList", user.getOrderList());
		model.addAttribute("userPayment", new UserPayment());
		model.addAttribute("userShipping", new UserShipping());
		model.addAttribute("listOfCreditCards", true);
		model.addAttribute("listOfShippingAddresses", true);
		model.addAttribute("classActiveEdit", true);
		model.addAttribute("stateList", USConstants.listOfUSStatesCode);
		return "myProfile";
	}

	@RequestMapping("/orderDetail")

	public String orderDetail(

			@RequestParam("id") Long orderId,

			Principal principal, Model model

	) {

		User user = userService.findByUsername(principal.getName());

		Order order = orderService.findOne(orderId);

		if (order.getUser().getId() != user.getId()) {

			return "badRequestPage";

		} else {

			List<CartItem> cartItemList = cartItemService.findByOrder(order);

			model.addAttribute("cartItemList", cartItemList);

			model.addAttribute("user", user);

			model.addAttribute("order", order);

			model.addAttribute("userPaymentList", user.getUserPaymentList());

			model.addAttribute("userShippingList", user.getUserShippingList());

			model.addAttribute("orderList", user.getOrderList());

			UserShipping userShipping = new UserShipping();

			model.addAttribute("userShipping", userShipping);

			List<String> stateList = USConstants.listOfUSStatesCode;

			Collections.sort(stateList);

			model.addAttribute("stateList", stateList);

			model.addAttribute("listOfShippingAddresses", true);

			model.addAttribute("classActiveOrders", true);

			model.addAttribute("listOfCreditCards", true);

			model.addAttribute("displayOrderDetail", true);

			return "myProfile";

		}

	}

}
