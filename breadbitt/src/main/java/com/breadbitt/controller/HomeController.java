package com.breadbitt.controller;

import java.security.Principal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
import com.breadbitt.service.impl.ProfileViewService;
import com.breadbitt.service.impl.CurrentCustomerService;
import com.breadbitt.service.impl.UserSecurityService;
import com.breadbitt.utility.MailConstructor;
import com.breadbitt.utility.SecurityUtility;
import com.breadbitt.utility.USConstants;

@Controller
public class HomeController {
	@Value("${app.base-url}")
	private String applicationBaseUrl;
	private static final SecureRandom RESET_RANDOM = new SecureRandom();
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
	@Autowired
	private ProfileViewService profileViewService;
	@Autowired
	private CurrentCustomerService currentCustomerService;

	private String generateResetToken() {
		byte[] bytes = new byte[32];
		RESET_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder()
				.withoutPadding()
				.encodeToString(bytes);
	}

	/** Resolve the authenticated customer through the shared identity service. */
	private User getCurrentUser(Principal principal) {
		if (principal == null) {
			return null;
		}
		return currentCustomerService.resolve(principal);
	}

	private String getCurrentUsername(Principal principal) {
		return currentCustomerService.resolveUsername(principal);
	}

	private void addStateList(Model model) {
		List<String> stateList = new ArrayList<>(USConstants.listOfUSStatesCode);
		Collections.sort(stateList);
		model.addAttribute("stateList", stateList);
	}

	private String populateProfile(
			Model model,
			String username,
			String activeTab) {
		ProfileViewService.ProfileData profile = profileViewService.loadProfile(username);
		if (profile == null) {
			return "redirect:/login";
		}
		model.addAttribute("user", profile.user());
		model.addAttribute(
				"userPaymentList", profile.payments());
		model.addAttribute(
				"userShippingList", profile.shipping());
		model.addAttribute(
				"orderList", profile.orders());
		if (!model.containsAttribute("userPayment")) {
			model.addAttribute(
					"userPayment", new UserPayment());
		}
		if (!model.containsAttribute("userShipping")) {
			model.addAttribute(
					"userShipping", new UserShipping());
		}
		if (!model.containsAttribute("userBilling")) {
			model.addAttribute(
					"userBilling", new UserBilling());
		}
		model.addAttribute("listOfCreditCards", true);
		model.addAttribute(
				"listOfShippingAddresses", true);
		addStateList(model);
		if ("billing".equals(activeTab)) {
			model.addAttribute(
					"classActiveBilling", true);
		} else if ("shipping".equals(activeTab)) {
			model.addAttribute(
					"classActiveShipping", true);
		} else if ("orders".equals(activeTab)) {
			model.addAttribute(
					"classActiveOrders", true);
		} else {
			model.addAttribute(
					"classActiveEdit", true);
		}
		return "myProfile";
	}

	@GetMapping("/")
	public String index() {
		return "index";
	}

	@GetMapping("/login")
	public String login(Model model) {
		model.addAttribute("classActiveLogin", true);
		return "myAccount";
	}

	@GetMapping("/breadshelf")
	public String breadShelfLegacyRedirect() {
		return "redirect:/breadShelf";
	}

	@GetMapping("/breadShelf")
	public String breadShelf(Model model) {
		List<Bread> breadList = breadService.findAll();
		model.addAttribute("breadList", breadList);
		return "breadShelf";
	}

	@GetMapping("/breadDetail")
	public String breadDetail(
			@RequestParam("id") Long id,
			Model model,
			Principal principal) {
		User user = getCurrentUser(principal);
		if (user != null) {
			model.addAttribute("user", user);
		}
		Optional<Bread> breadOptional = breadService.findById(id);
		if (breadOptional.isEmpty()) {
			return "redirect:/error";
		}
		model.addAttribute(
				"bread", breadOptional.get());
		model.addAttribute(
				"qtyList",
				Arrays.asList(1, 2, 3, 4, 5,
						6, 7, 8, 9, 10));
		model.addAttribute("qty", 1);
		return "breadDetail";
	}

	@PostMapping("/forgetPassword")
	public String forgetPassword(
			@RequestParam("email") String email,
			Model model) {
		model.addAttribute(
				"classActiveForgetPassword", true);
		if (email != null && !email.isBlank()) {
			User user = userService.findByEmail(
					email.trim());
			if (user != null) {
				String token = generateResetToken();
				userService.createPasswordResetTokenForUser(
						user, token);
				SimpleMailMessage message = mailConstructor.constructPasswordSetupEmail(
						applicationBaseUrl,
						token,
						user,
						false);
				mailSender.send(message);
			}
		}
		model.addAttribute(
				"forgetPasswordEmailSent", true);
		return "myAccount";
	}

	@GetMapping("/myProfile")
	public String myProfile(
			Model model,
			Principal principal) {
		if (principal == null) {
			return "redirect:/login";
		}
		return populateProfile(
				model, getCurrentUsername(principal), "edit");
	}

	@GetMapping("/listOfCreditCards")
	public String listOfCreditCards(
			Model model,
			Principal principal) {
		if (principal == null) {
			return "redirect:/login";
		}
		return populateProfile(
				model, getCurrentUsername(principal), "billing");
	}

	@GetMapping("/listOfShippingAddresses")
	public String listOfShippingAddresses(
			Model model,
			Principal principal) {
		if (principal == null) {
			return "redirect:/login";
		}
		return populateProfile(
				model, getCurrentUsername(principal), "shipping");
	}

	@GetMapping("/addNewCreditCard")
	public String addNewCreditCard(
			Model model,
			Principal principal) {
		if (principal == null) {
			return "redirect:/login";
		}
		model.addAttribute(
				"userPayment", new UserPayment());
		model.addAttribute(
				"userBilling", new UserBilling());
		model.addAttribute("addNewCreditCard", true);
		return populateProfile(
				model, getCurrentUsername(principal), "billing");
	}

	@PostMapping("/addNewCreditCard")
	public String addNewCreditCardPost(
			@ModelAttribute("userPayment") UserPayment userPayment,
			@ModelAttribute("userBilling") UserBilling userBilling,
			Principal principal) {
		User user = getCurrentUser(principal);
		if (user == null) {
			return "redirect:/login";
		}
		userPayment.setUser(user);
		userService.updateUserBilling(
				userBilling, userPayment, user);
		return "redirect:/listOfCreditCards";
	}

	@GetMapping("/updateCreditCard")
	public String updateCreditCard(
			@RequestParam("id") Long creditCardId,
			Principal principal,
			Model model) {
		User user = getCurrentUser(principal);
		if (user == null) {
			return "redirect:/login";
		}
		UserPayment userPayment = userPaymentService.findById(creditCardId);
		if (userPayment == null
				|| userPayment.getUser() == null
				|| !Objects.equals(
						user.getId(),
						userPayment.getUser().getId())) {
			return "badRequestPage";
		}
		model.addAttribute(
				"userPayment", userPayment);
		UserBilling userBilling = userPayment.getUserBilling();
		model.addAttribute(
				"userBilling",
				userBilling != null
						? userBilling
						: new UserBilling());
		model.addAttribute("addNewCreditCard", true);
		return populateProfile(
				model, getCurrentUsername(principal), "billing");
	}

	@PostMapping("/setDefaultPayment")
	public String setDefaultPayment(
			@RequestParam("defaultUserPaymentId") Long defaultPaymentId,
			Principal principal) {
		User user = getCurrentUser(principal);
		if (user == null) {
			return "redirect:/login";
		}
		UserPayment payment = userPaymentService.findById(defaultPaymentId);
		if (payment == null
				|| payment.getUser() == null
				|| !Objects.equals(
						user.getId(),
						payment.getUser().getId())) {
			return "badRequestPage";
		}
		userService.setUserDefaultPayment(
				defaultPaymentId, user);
		return "redirect:/listOfCreditCards";
	}

	@PostMapping("/removeCreditCard")
	public String removeCreditCard(
			@RequestParam("id") Long creditCardId,
			Principal principal) {
		User user = getCurrentUser(principal);
		if (user == null) {
			return "redirect:/login";
		}
		UserPayment userPayment = userPaymentService.findById(creditCardId);
		if (userPayment == null
				|| userPayment.getUser() == null
				|| !Objects.equals(
						user.getId(),
						userPayment.getUser().getId())) {
			return "badRequestPage";
		}
		userPaymentService.removeById(creditCardId);
		return "redirect:/listOfCreditCards";
	}

	@GetMapping("/addNewShippingAddress")
	public String addNewShippingAddress(
			Model model,
			Principal principal) {
		if (principal == null) {
			return "redirect:/login";
		}
		model.addAttribute(
				"userShipping", new UserShipping());
		model.addAttribute(
				"addNewShippingAddress", true);
		return populateProfile(
				model, getCurrentUsername(principal), "shipping");
	}

	@PostMapping("/addNewShippingAddress")
	public String addNewShippingAddressPost(
			@ModelAttribute("userShipping") UserShipping userShipping,
			Principal principal) {
		User user = getCurrentUser(principal);
		if (user == null) {
			return "redirect:/login";
		}
		userShipping.setUser(user);
		userService.updateUserShipping(
				userShipping, user);
		return "redirect:/listOfShippingAddresses";
	}

	@GetMapping("/updateUserShipping")
	public String updateUserShipping(
			@RequestParam("id") Long shippingAddressId,
			Principal principal,
			Model model) {
		User user = getCurrentUser(principal);
		if (user == null) {
			return "redirect:/login";
		}
		UserShipping userShipping = userShippingService.findById(
				shippingAddressId);
		if (userShipping == null
				|| userShipping.getUser() == null
				|| !Objects.equals(
						user.getId(),
						userShipping.getUser().getId())) {
			return "badRequestPage";
		}
		model.addAttribute(
				"userShipping", userShipping);
		model.addAttribute(
				"addNewShippingAddress", true);
		return populateProfile(
				model, getCurrentUsername(principal), "shipping");
	}

	@PostMapping("/setDefaultShippingAddress")
	public String setDefaultShippingAddress(
			@RequestParam("defaultShippingAddressId") Long defaultShippingId,
			Principal principal) {
		User user = getCurrentUser(principal);
		if (user == null) {
			return "redirect:/login";
		}
		UserShipping shipping = userShippingService.findById(
				defaultShippingId);
		if (shipping == null
				|| shipping.getUser() == null
				|| !Objects.equals(
						user.getId(),
						shipping.getUser().getId())) {
			return "badRequestPage";
		}
		userService.setUserDefaultShipping(
				defaultShippingId, user);
		return "redirect:/listOfShippingAddresses";
	}

	@PostMapping("/removeUserShipping")
	public String removeUserShipping(
			@RequestParam("id") Long userShippingId,
			Principal principal) {
		User user = getCurrentUser(principal);
		if (user == null) {
			return "redirect:/login";
		}
		UserShipping userShipping = userShippingService.findById(userShippingId);
		if (userShipping == null
				|| userShipping.getUser() == null
				|| !Objects.equals(
						user.getId(),
						userShipping.getUser().getId())) {
			return "badRequestPage";
		}
		userShippingService.removeById(userShippingId);
		return "redirect:/listOfShippingAddresses";
	}

	@PostMapping("/newUser")
	public String newUserPost(
			HttpServletRequest request,
			@ModelAttribute("email") String userEmail,
			@ModelAttribute("username") String username,
			Model model) throws Exception {
		model.addAttribute(
				"classActiveNewAccount", true);
		model.addAttribute("email", userEmail);
		model.addAttribute("username", username);
		if (username == null
				|| username.isBlank()
				|| userEmail == null
				|| userEmail.isBlank()) {
			model.addAttribute(
					"registrationError", true);
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
		user.setPassword(
				SecurityUtility.passwordEncoder()
						.encode(generateResetToken()));
		Role role = new Role();
		role.setRoleId(1);
		role.setName("ROLE_USER");
		Set<UserRole> userRoles = new HashSet<>();
		userRoles.add(new UserRole(user, role));
		user = userService.createUser(
				user, userRoles);
		String token = generateResetToken();
		userService.createPasswordResetTokenForUser(
				user, token);
		SimpleMailMessage email = mailConstructor.constructPasswordSetupEmail(
				applicationBaseUrl,
				token,
				user,
				true);
		mailSender.send(email);
		model.addAttribute("emailSent", true);
		return "myAccount";
	}

	@GetMapping("/newUser")
	public String newUser(
			@RequestParam("token") String token) {
		return "redirect:/resetPassword?token="
				+ java.net.URLEncoder.encode(
						token,
						java.nio.charset.StandardCharsets.UTF_8);
	}

	@GetMapping("/resetPassword")
	public String showResetPassword(
			@RequestParam("token") String token,
			Model model) {
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
		if (newPassword == null
				|| !newPassword.equals(confirmPassword)) {
			model.addAttribute(
					"passwordMismatch", true);
			model.addAttribute("token", token);
			return "resetPassword";
		}
		if (newPassword.length() < 12
				|| newPassword.length() > 128) {
			model.addAttribute(
					"passwordLengthInvalid", true);
			model.addAttribute("token", token);
			return "resetPassword";
		}
		if (!userService.resetPassword(
				token, newPassword)) {
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
		User currentUser = getCurrentUser(principal);
		if (currentUser == null) {
			return "redirect:/login";
		}
		if (submittedUser.getEmail() == null
				|| submittedUser.getEmail().isBlank()
				|| submittedUser.getUsername() == null
				|| submittedUser.getUsername().isBlank()) {
			model.addAttribute(
					"profileValidationError", true);
			return populateProfile(
					model,
					getCurrentUsername(principal),
					"edit");
		}
		User emailOwner = userService.findByEmail(
				submittedUser.getEmail());
		if (emailOwner != null
				&& !Objects.equals(
						emailOwner.getId(),
						currentUser.getId())) {
			model.addAttribute("emailExists", true);
			return populateProfile(
					model,
					getCurrentUsername(principal),
					"edit");
		}
		User usernameOwner = userService.findByUsername(
				submittedUser.getUsername());
		if (usernameOwner != null
				&& !Objects.equals(
						usernameOwner.getId(),
						currentUser.getId())) {
			model.addAttribute("usernameExists", true);
			return populateProfile(
					model,
					getCurrentUsername(principal),
					"edit");
		}
		if (newPassword != null
				&& !newPassword.isBlank()) {
			if (newPassword.length() < 12
					|| newPassword.length() > 128) {
				model.addAttribute(
						"passwordLengthInvalid", true);
				return populateProfile(
						model,
						getCurrentUsername(principal),
						"edit");
			}
			BCryptPasswordEncoder encoder = SecurityUtility.passwordEncoder();
			if (submittedUser.getPassword() == null
					|| !encoder.matches(
							submittedUser.getPassword(),
							currentUser.getPassword())) {
				model.addAttribute(
						"incorrectPassword", true);
				return populateProfile(
						model,
						getCurrentUsername(principal),
						"edit");
			}
			currentUser.setPassword(
					encoder.encode(newPassword));
		}
		currentUser.setFirstName(
				submittedUser.getFirstName());
		currentUser.setLastName(
				submittedUser.getLastName());
		currentUser.setUsername(
				submittedUser.getUsername());
		currentUser.setEmail(
				submittedUser.getEmail());
		userService.save(currentUser);

		if (!(principal instanceof Authentication existingAuthentication
				&& existingAuthentication.getPrincipal() instanceof OidcUser)) {
			UserDetails details = userSecurityService.loadUserByUsername(
					currentUser.getUsername());
			Authentication authentication = new UsernamePasswordAuthenticationToken(
					details,
					null,
					details.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}
		model.addAttribute("updateSuccess", true);
		return populateProfile(
				model,
				currentUser.getUsername(),
				"edit");
	}

	@GetMapping("/orderDetail")
	public String orderDetail(
			@RequestParam("id") Long orderId,
			Principal principal,
			Model model) {
		User user = getCurrentUser(principal);
		if (user == null) {
			return "redirect:/login";
		}
		Order order = orderService.findOne(orderId);
		if (order == null
				|| order.getUser() == null
				|| !Objects.equals(
						order.getUser().getId(),
						user.getId())) {
			return "badRequestPage";
		}
		List<CartItem> cartItemList = cartItemService.findByOrder(order);
		model.addAttribute(
				"cartItemList", cartItemList);
		model.addAttribute("order", order);
		model.addAttribute("displayOrderDetail", true);
		return populateProfile(
				model,
				getCurrentUsername(principal),
				"orders");
	}
}
