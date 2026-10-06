package com.breadbitt.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

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
import com.breadbitt.service.BillingAddressService;
import com.breadbitt.service.CartItemService;
import com.breadbitt.service.OrderService;
import com.breadbitt.service.PaymentService;
import com.breadbitt.service.ShippingAddressService;
import com.breadbitt.service.ShoppingCartService;
import com.breadbitt.service.UserPaymentService;
import com.breadbitt.service.UserService;
import com.breadbitt.service.UserShippingService;
import com.breadbitt.utility.MailConstructor;
import com.breadbitt.utility.USConstants;

@Controller
public class CheckoutController {

	private ShippingAddress shippingAddress = new ShippingAddress();
	private BillingAddress billingAddress = new BillingAddress();
	private Payment payment = new Payment();
	
	@Autowired
	private JavaMailSender mailSender;
	
	@Autowired
	private MailConstructor mailConstructor;
	
	@Autowired
	private UserService userService;

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
	
	@RequestMapping("/checkout")
	public String checkout(
			@RequestParam("id") Long cartId,
			@RequestParam(value="missingRequiredField", required=false) boolean missingRequiredField,
			Model model, Principal principal
			){
		User user = userService.findByUsername(principal.getName());
		
		if(cartId != user.getShoppingCart().getId()) {
			return "badRequestPage";
		}
		
		List<CartItem> cartItemList = cartItemService.findByShoppingCart(user.getShoppingCart());
		
		if(cartItemList.size() == 0) {
			model.addAttribute("emptyCart", true);
			return "forward:/shoppintCart/cart";
		}
		
		for (CartItem cartItem : cartItemList) {
			if(cartItem.getBread().getInStockNumber() < cartItem.getQty()) {
				model.addAttribute("notEnoughStock", true);
				return "forward:/shoppingCart/cart";
			}
		}
		
		List<UserShipping> userShippingList = user.getUserShippingList();
		List<UserPayment> userPaymentList = user.getUserPaymentList();
		
		model.addAttribute("userShippingList", userShippingList);
		model.addAttribute("userPaymentList", userPaymentList);
		
		if (userPaymentList.size() == 0) {
			model.addAttribute("emptyPaymentList", true);
		} else {
			model.addAttribute("emptyPaymentList", false);
		}
		
		if (userShippingList.size() == 0) {
			model.addAttribute("emptyShippingList", true);
		} else {
			model.addAttribute("emptyShippingList", false);
		}
		
		@SuppressWarnings("unused")
		ShoppingCart shoppingCart = user.getShoppingCart();
		
		for(UserShipping userShipping : userShippingList) {
			if(userShipping.isUserShippingDefault()) {
				shippingAddressService.setByUserShipping(userShipping, shippingAddress);
			}
		}
		
		for (UserPayment userPayment : userPaymentList) {
			if(userPayment.isDefaultPayment()) {
				paymentService.setByUserPayment(userPayment, payment);
				billingAddressService.setByUserBilling(userPayment.getUserBilling(), billingAddress);
			}
		}
		
		model.addAttribute("shippingAddress", shippingAddress);
		model.addAttribute("payment", payment);
		model.addAttribute("billingAddress", billingAddress);
		model.addAttribute("cartItemList", cartItemList);
		model.addAttribute("shoppingCart", user.getShoppingCart());
		
		List<String> stateList = USConstants.listOfUSStatesCode;
		Collections.sort(stateList);
		model.addAttribute("stateList", stateList);
		
		model.addAttribute("classActiveShipping", true);
		
		if(missingRequiredField) {
			model.addAttribute("missingRequiredField", true);
		}
		
		return "checkout";
		
	}
	
	@RequestMapping(value = "/checkout", method = RequestMethod.POST)
	public String checkoutPost(@ModelAttribute("shippingAddress") ShippingAddress shippingAddress,
	                           @ModelAttribute("billingAddress") BillingAddress billingAddress,
	                           @ModelAttribute("payment") Payment payment,
	                           @ModelAttribute("billingSameAsShipping") String billingSameAsShipping,
	                           @ModelAttribute("shippingMethod") String shippingMethod, Principal principal, Model model) {
	    ShoppingCart shoppingCart = userService.findByUsername(principal.getName()).getShoppingCart();
	    List<CartItem> cartItemList = cartItemService.findByShoppingCart(shoppingCart);
	    model.addAttribute("cartItemList", cartItemList);

	    if ("true".equals(billingSameAsShipping)) {
	        billingAddress.setBillingAddressName(shippingAddress.getShippingAddressName());
	        billingAddress.setBillingAddressStreet1(shippingAddress.getShippingAddressStreet1());
	        billingAddress.setBillingAddressStreet2(shippingAddress.getShippingAddressStreet2());
	        billingAddress.setBillingAddressCity(shippingAddress.getShippingAddressCity());
	        billingAddress.setBillingAddressState(shippingAddress.getShippingAddressState());
	        billingAddress.setBillingAddressCountry(shippingAddress.getShippingAddressCountry());
	        billingAddress.setBillingAddressZipcode(shippingAddress.getShippingAddressZipcode());
	    }

	    if (isAnyRequiredFieldEmpty(shippingAddress, billingAddress, payment)) {
	        model.addAttribute("missingRequiredField", true);
	        return "redirect:/checkout?id=" + shoppingCart.getId() + "&missingRequiredField=true";
	    }

	    User user = userService.findByUsername(principal.getName());
	    Order order = orderService.createOrder(shoppingCart, shippingAddress, billingAddress, payment, shippingMethod, user);

	    mailSender.send(mailConstructor.constructOrderConfirmationEmail(user, order, Locale.ENGLISH));
	    shoppingCartService.clearShoppingCart(shoppingCart);

	    LocalDate today = LocalDate.now();
	    LocalDate estimatedDeliveryDate = "groundShipping".equals(shippingMethod) ? today.plusDays(5) : today.plusDays(3);
	    model.addAttribute("estimatedDeliveryDate", estimatedDeliveryDate);

	    return "orderSubmittedPage";
	}

	private boolean isAnyRequiredFieldEmpty(ShippingAddress shippingAddress, BillingAddress billingAddress, Payment payment) {
	    return isNullOrEmpty(shippingAddress.getShippingAddressStreet1())
	            || isNullOrEmpty(shippingAddress.getShippingAddressCity())
	            || isNullOrEmpty(shippingAddress.getShippingAddressState())
	            || isNullOrEmpty(shippingAddress.getShippingAddressName())
	            || isNullOrEmpty(shippingAddress.getShippingAddressZipcode())
	            || isNullOrEmpty(payment.getCardNumber())
	            || payment.getCvc() == 0
	            || isNullOrEmpty(billingAddress.getBillingAddressStreet1())
	            || isNullOrEmpty(billingAddress.getBillingAddressCity())
	            || isNullOrEmpty(billingAddress.getBillingAddressState())
	            || isNullOrEmpty(billingAddress.getBillingAddressName())
	            || isNullOrEmpty(billingAddress.getBillingAddressZipcode());
	}

	private boolean isNullOrEmpty(String str) {
	    return str == null || str.isEmpty();
	}


	
	@RequestMapping("/setShippingAddress")
	public String setShippingAddress(@RequestParam("userShippingId") Long userShippingId,
	                                 Principal principal, Model model) {
	    User user = userService.findByUsername(principal.getName());
	    UserShipping userShipping = userShippingService.findById(userShippingId);

	    if (userShipping.getUser().getId() != user.getId()) {
	        return "badRequestPage";
	    } else {
	        shippingAddressService.setByUserShipping(userShipping, shippingAddress);

	        List<CartItem> cartItemList = cartItemService.findByShoppingCart(user.getShoppingCart());
	        model.addAttribute("cartItemList", cartItemList);
	        model.addAttribute("shippingAddress", shippingAddress);
	        model.addAttribute("payment", payment);
	        model.addAttribute("billingAddress", billingAddress);
	        model.addAttribute("shoppingCart", user.getShoppingCart());

	        List<String> stateList = USConstants.listOfUSStatesCode;
	        Collections.sort(stateList);
	        model.addAttribute("stateList", stateList);

	        List<UserShipping> userShippingList = user.getUserShippingList();
	        List<UserPayment> userPaymentList = user.getUserPaymentList();
	        model.addAttribute("userShippingList", userShippingList);
	        model.addAttribute("userPaymentList", userPaymentList);

	        model.addAttribute("classActiveShipping", true);
	        model.addAttribute("emptyPaymentList", userPaymentList.isEmpty());
	        model.addAttribute("emptyShippingList", false);

	        return "checkout";
	    }
	}

	@RequestMapping("/setPaymentMethod")
	public String setPaymentMethod(@RequestParam("userPaymentId") Long userPaymentId,
	                               Principal principal, Model model) {
	    User user = userService.findByUsername(principal.getName());
	    UserPayment userPayment = userPaymentService.findById(userPaymentId);
	    UserBilling userBilling = userPayment.getUserBilling();

	    if (userPayment.getUser().getId() != user.getId()) {
	        return "badRequestPage";
	    } else {
	        paymentService.setByUserPayment(userPayment, payment);

	        List<CartItem> cartItemList = cartItemService.findByShoppingCart(user.getShoppingCart());
	        billingAddressService.setByUserBilling(userBilling, billingAddress);

	        model.addAttribute("cartItemList", cartItemList);
	        model.addAttribute("shippingAddress", shippingAddress);
	        model.addAttribute("payment", payment);
	        model.addAttribute("billingAddress", billingAddress);
	        model.addAttribute("shoppingCart", user.getShoppingCart());

	        List<String> stateList = USConstants.listOfUSStatesCode;
	        Collections.sort(stateList);
	        model.addAttribute("stateList", stateList);

	        List<UserShipping> userShippingList = user.getUserShippingList();
	        List<UserPayment> userPaymentList = user.getUserPaymentList();
	        model.addAttribute("userShippingList", userShippingList);
	        model.addAttribute("userPaymentList", userPaymentList);

	        model.addAttribute("classActivePayment", true);
	        model.addAttribute("emptyPaymentList", false);
	        model.addAttribute("emptyShippingList", userShippingList.isEmpty());

	        return "checkout";
	    }
	}

	
}
