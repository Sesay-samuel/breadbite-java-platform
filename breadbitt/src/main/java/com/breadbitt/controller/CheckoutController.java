package com.breadbitt.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import com.breadbitt.domain.BillingAddress;
import com.breadbitt.domain.CartItem;
import com.breadbitt.domain.Order;
import com.breadbitt.domain.Payment;
import com.breadbitt.domain.ShippingAddress;
import com.breadbitt.domain.ShoppingCart;
import com.breadbitt.domain.User;
import com.breadbitt.domain.UserBilling;
import com.breadbitt.domain.UserPayment;
import com.breadbitt.domain.UserShipping;
import com.breadbitt.repository.UserPaymentRepository;
import com.breadbitt.repository.UserShippingRepository;
import com.breadbitt.service.BillingAddressService;
import com.breadbitt.service.CartItemService;
import com.breadbitt.service.OrderService;
import com.breadbitt.service.PaymentService;
import com.breadbitt.service.ShippingAddressService;
import com.breadbitt.service.ShoppingCartService;
import com.breadbitt.service.UserPaymentService;
import com.breadbitt.service.UserShippingService;
import com.breadbitt.service.impl.CurrentCustomerService;
import com.breadbitt.utility.MailConstructor;
import com.breadbitt.utility.USConstants;

@Controller
@SessionAttributes({
		"shippingAddress",
		"billingAddress",
		"payment"
})
public class CheckoutController {
	private static final Logger LOG = LoggerFactory.getLogger(CheckoutController.class);
	@Autowired
	private JavaMailSender mailSender;
	@Autowired
	private MailConstructor mailConstructor;
	@Autowired
	private CurrentCustomerService currentCustomerService;
	@Autowired
	private CartItemService cartItemService;
	@Autowired
	private ShoppingCartService shoppingCartService;
	@Autowired
	private ShippingAddressService shippingAddressService;
	@Autowired
	private BillingAddressService billingAddressService;
	@Autowired
	private PaymentService paymentService;
	@Autowired
	private UserShippingService userShippingService;
	@Autowired
	private UserPaymentService userPaymentService;
	@Autowired
	private OrderService orderService;
	// Existing repository methods used instead of
	// unsupported findByUser(User) service methods.
	@Autowired
	private UserShippingRepository userShippingRepository;
	@Autowired
	private UserPaymentRepository userPaymentRepository;

	// ========================================
	// CHECKOUT FORM OBJECTS
	// ========================================
	@ModelAttribute("shippingAddress")
	public ShippingAddress shippingAddress() {
		return new ShippingAddress();
	}

	@ModelAttribute("billingAddress")
	public BillingAddress billingAddress() {
		return new BillingAddress();
	}

	@ModelAttribute("payment")
	public Payment payment() {
		return new Payment();
	}

	// ========================================
	// CURRENT CUSTOMER
	// ========================================
	private User getCurrentUser(Principal principal) {
		if (principal == null) {
			return null;
		}
		return currentCustomerService.resolve(principal);
	}

	// ========================================
	// CURRENT CUSTOMER CART
	// ========================================
	private ShoppingCart getCustomerCart(User user) {
		if (user == null) {
			return null;
		}
		return user.getShoppingCart();
	}

	// ========================================
	// LOAD CUSTOMER SHIPPING ADDRESSES
	// ========================================
	private List<UserShipping> getCustomerShipping(
			User user) {
		if (user == null || user.getId() == null) {
			return new ArrayList<>();
		}
		return userShippingRepository.findByUser_Id(
				user.getId());
	}

	// ========================================
	// LOAD CUSTOMER PAYMENT METHODS
	// ========================================
	private List<UserPayment> getCustomerPayments(
			User user) {
		if (user == null || user.getId() == null) {
			return new ArrayList<>();
		}
		return userPaymentRepository.findByUser_Id(
				user.getId());
	}

	// ========================================
	// CHECK INVENTORY
	// ========================================
	private boolean hasInsufficientStock(
			List<CartItem> cartItemList) {
		if (cartItemList == null || cartItemList.isEmpty()) {
			return true;
		}
		for (CartItem cartItem : cartItemList) {
			if (cartItem == null
					|| cartItem.getBread() == null
					|| cartItem.getQty() < 1
					|| cartItem.getBread().getInStockNumber() < cartItem.getQty()) {
				return true;
			}
		}
		return false;
	}

	// ========================================
	// PREPARE CHECKOUT PAGE
	// ========================================
	private void populateCheckoutModel(
			Model model,
			User user,
			ShoppingCart shoppingCart,
			List<CartItem> cartItemList,
			boolean missingRequiredField) {
		List<UserShipping> userShippingList = getCustomerShipping(user);
		List<UserPayment> userPaymentList = getCustomerPayments(user);
		model.addAttribute(
				"userShippingList",
				userShippingList);
		model.addAttribute(
				"userPaymentList",
				userPaymentList);
		model.addAttribute(
				"emptyShippingList",
				userShippingList.isEmpty());
		model.addAttribute(
				"emptyPaymentList",
				userPaymentList.isEmpty());
		model.addAttribute(
				"cartItemList",
				cartItemList);
		model.addAttribute(
				"shoppingCart",
				shoppingCart);
		List<String> stateList = new ArrayList<>(
				USConstants.listOfUSStatesCode);
		stateList.sort(String::compareTo);
		model.addAttribute("stateList", stateList);
		if (missingRequiredField) {
			model.addAttribute(
					"missingRequiredField", true);
		}
	}

	// ========================================
	// DISPLAY CHECKOUT PAGE
	// ========================================
	@GetMapping("/checkout")
	public String checkout(
			@RequestParam("id") Long cartId,
			@RequestParam(value = "missingRequiredField", required = false, defaultValue = "false") boolean missingRequiredField,
			Model model,
			Principal principal,
			@ModelAttribute("shippingAddress") ShippingAddress shippingAddress,
			@ModelAttribute("billingAddress") BillingAddress billingAddress,
			@ModelAttribute("payment") Payment payment) {
		User user = getCurrentUser(principal);
		if (user == null) {
			return "redirect:/login";
		}
		ShoppingCart shoppingCart = getCustomerCart(user);
		if (shoppingCart == null
				|| !Objects.equals(
						cartId,
						shoppingCart.getId())) {
			return "badRequestPage";
		}
		List<CartItem> cartItemList = cartItemService.findByShoppingCart(
				shoppingCart);
		// Do not permit empty-cart checkout.
		if (cartItemList == null
				|| cartItemList.isEmpty()) {
			return "redirect:/shoppingCart/cart";
		}
		// Recheck stock.
		if (hasInsufficientStock(cartItemList)) {
			return "redirect:/shoppingCart/cart";
		}
		populateCheckoutModel(
				model,
				user,
				shoppingCart,
				cartItemList,
				missingRequiredField);
		// Load addresses and payment methods using
		// authenticated-user-scoped repository queries.
		List<UserShipping> userShippingList = getCustomerShipping(user);
		List<UserPayment> userPaymentList = getCustomerPayments(user);
		// Apply default shipping address.
		for (UserShipping userShipping : userShippingList) {
			if (userShipping.isUserShippingDefault()) {
				shippingAddressService.setByUserShipping(
						userShipping,
						shippingAddress);
				break;
			}
		}
		// Apply default payment and billing address.
		for (UserPayment userPayment : userPaymentList) {
			if (userPayment.isDefaultPayment()) {
				paymentService.setByUserPayment(
						userPayment,
						payment);
				UserBilling userBilling = userPayment.getUserBilling();
				if (userBilling != null) {
					billingAddressService.setByUserBilling(
							userBilling,
							billingAddress);
				}
				break;
			}
		}
		model.addAttribute(
				"classActiveShipping", true);
		return "checkout";
	}

	// ========================================
	// SUBMIT ORDER
	// ========================================
	@PostMapping("/checkout")
	public String checkoutPost(
			@ModelAttribute("shippingAddress") ShippingAddress shippingAddress,
			@ModelAttribute("billingAddress") BillingAddress billingAddress,
			@ModelAttribute("payment") Payment payment,
			@RequestParam(value = "billingSameAsShipping", required = false, defaultValue = "false") String billingSameAsShipping,
			@RequestParam(value = "shippingMethod", required = false, defaultValue = "") String shippingMethod,
			Principal principal,
			Model model,
			SessionStatus sessionStatus) {
		User user = getCurrentUser(principal);
		if (user == null) {
			return "redirect:/login";
		}
		ShoppingCart shoppingCart = getCustomerCart(user);
		if (shoppingCart == null) {
			return "badRequestPage";
		}
		List<CartItem> cartItemList = cartItemService.findByShoppingCart(
				shoppingCart);
		if (cartItemList == null
				|| cartItemList.isEmpty()) {
			return "redirect:/shoppingCart/cart";
		}
		if (hasInsufficientStock(cartItemList)) {
			return "redirect:/shoppingCart/cart";
		}
		// Accept only supported shipping methods.
		if (!"groundShipping".equals(shippingMethod)
				&& !"premiumShipping".equals(shippingMethod)) {
			return "redirect:/checkout?id="
					+ shoppingCart.getId()
					+ "&missingRequiredField=true";
		}
		// Copy shipping details when requested.
		if ("true".equalsIgnoreCase(
				billingSameAsShipping)) {
			billingAddress.setBillingAddressName(
					shippingAddress.getShippingAddressName());
			billingAddress.setBillingAddressStreet1(
					shippingAddress.getShippingAddressStreet1());
			billingAddress.setBillingAddressStreet2(
					shippingAddress.getShippingAddressStreet2());
			billingAddress.setBillingAddressCity(
					shippingAddress.getShippingAddressCity());
			billingAddress.setBillingAddressState(
					shippingAddress.getShippingAddressState());
			billingAddress.setBillingAddressCountry(
					shippingAddress.getShippingAddressCountry());
			billingAddress.setBillingAddressZipcode(
					shippingAddress.getShippingAddressZipcode());
		}
		// Check required checkout details.
		if (isAnyRequiredFieldEmpty(
				shippingAddress,
				billingAddress,
				payment)) {
			return "redirect:/checkout?id="
					+ shoppingCart.getId()
					+ "&missingRequiredField=true";
		}
		// OrderService must independently validate
		// prices, stock, and customer ownership.
		Order order = orderService.createOrder(
				shoppingCart,
				shippingAddress,
				billingAddress,
				payment,
				shippingMethod,
				user);
		// Clear cart after successful order creation.
		shoppingCartService.clearShoppingCart(
				shoppingCart);
		LocalDate today = LocalDate.now();
		LocalDate estimatedDeliveryDate = "groundShipping".equals(shippingMethod)
				? today.plusDays(5)
				: today.plusDays(3);
		model.addAttribute(
				"estimatedDeliveryDate",
				estimatedDeliveryDate);
		model.addAttribute("order", order);
		// Clear checkout objects from the session.
		sessionStatus.setComplete();
		// Email failure must not undo an order.
		try {
			mailSender.send(
					mailConstructor.constructOrderConfirmationEmail(
							user,
							order,
							Locale.ENGLISH));
		} catch (MailException exception) {
			LOG.warn(
					"Order confirmation email could not be sent for order {}",
					order.getId());
		}
		return "orderSubmittedPage";
	}

	// ========================================
	// SELECT SAVED SHIPPING ADDRESS
	// ========================================
	@GetMapping("/setShippingAddress")
	public String setShippingAddress(
			@RequestParam("userShippingId") Long userShippingId,
			Principal principal,
			Model model,
			@ModelAttribute("shippingAddress") ShippingAddress shippingAddress) {
		User user = getCurrentUser(principal);
		if (user == null) {
			return "redirect:/login";
		}
		ShoppingCart shoppingCart = getCustomerCart(user);
		if (shoppingCart == null) {
			return "badRequestPage";
		}
		UserShipping userShipping = userShippingService.findById(
				userShippingId);
		if (userShipping == null
				|| userShipping.getUser() == null
				|| !Objects.equals(
						userShipping.getUser().getId(),
						user.getId())) {
			return "badRequestPage";
		}
		shippingAddressService.setByUserShipping(
				userShipping,
				shippingAddress);
		List<CartItem> cartItemList = cartItemService.findByShoppingCart(
				shoppingCart);
		populateCheckoutModel(
				model,
				user,
				shoppingCart,
				cartItemList,
				false);
		model.addAttribute(
				"classActiveShipping", true);
		return "checkout";
	}

	// ========================================
	// SELECT SAVED PAYMENT METHOD
	// ========================================
	@GetMapping("/setPaymentMethod")
	public String setPaymentMethod(
			@RequestParam("userPaymentId") Long userPaymentId,
			Principal principal,
			Model model,
			@ModelAttribute("payment") Payment payment,
			@ModelAttribute("billingAddress") BillingAddress billingAddress) {
		User user = getCurrentUser(principal);
		if (user == null) {
			return "redirect:/login";
		}
		ShoppingCart shoppingCart = getCustomerCart(user);
		if (shoppingCart == null) {
			return "badRequestPage";
		}
		UserPayment userPayment = userPaymentService.findById(
				userPaymentId);
		if (userPayment == null
				|| userPayment.getUser() == null
				|| !Objects.equals(
						userPayment.getUser().getId(),
						user.getId())) {
			return "badRequestPage";
		}
		paymentService.setByUserPayment(
				userPayment,
				payment);
		UserBilling userBilling = userPayment.getUserBilling();
		if (userBilling != null) {
			billingAddressService.setByUserBilling(
					userBilling,
					billingAddress);
		}
		List<CartItem> cartItemList = cartItemService.findByShoppingCart(
				shoppingCart);
		populateCheckoutModel(
				model,
				user,
				shoppingCart,
				cartItemList,
				false);
		model.addAttribute(
				"classActivePayment", true);
		return "checkout";
	}

	// ========================================
	// REQUIRED FIELD VALIDATION
	// ========================================
	private boolean isAnyRequiredFieldEmpty(
			ShippingAddress shippingAddress,
			BillingAddress billingAddress,
			Payment payment) {
		return shippingAddress == null
				|| billingAddress == null
				|| payment == null
				|| isNullOrEmpty(
						shippingAddress.getShippingAddressName())
				|| isNullOrEmpty(
						shippingAddress.getShippingAddressStreet1())
				|| isNullOrEmpty(
						shippingAddress.getShippingAddressCity())
				|| isNullOrEmpty(
						shippingAddress.getShippingAddressState())
				|| isNullOrEmpty(
						shippingAddress.getShippingAddressZipcode())
				|| isNullOrEmpty(
						billingAddress.getBillingAddressName())
				|| isNullOrEmpty(
						billingAddress.getBillingAddressStreet1())
				|| isNullOrEmpty(
						billingAddress.getBillingAddressCity())
				|| isNullOrEmpty(
						billingAddress.getBillingAddressState())
				|| isNullOrEmpty(
						billingAddress.getBillingAddressZipcode())
				|| isNullOrEmpty(
						payment.getCardNumber())
				|| payment.getCvc() == 0;
	}

	private boolean isNullOrEmpty(String value) {
		return value == null || value.trim().isEmpty();
	}
}
