
package com.breadbitt.controller;

import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.breadbitt.domain.Bread;
import com.breadbitt.domain.CartItem;
import com.breadbitt.domain.ShoppingCart;
import com.breadbitt.domain.User;

import com.breadbitt.service.BreadService;
import com.breadbitt.service.CartItemService;
import com.breadbitt.service.ShoppingCartService;
import com.breadbitt.service.impl.CurrentCustomerService;

@Controller
@RequestMapping("/shoppingCart")
public class ShoppingCartController {

	@Autowired
	private CurrentCustomerService currentCustomerService;

	@Autowired
	private CartItemService cartItemService;

	@Autowired
	private BreadService breadService;

	@Autowired
	private ShoppingCartService shoppingCartService;

	private User getAuthenticatedUser(Principal principal) {

		if (principal == null) {
			return null;
		}

		return currentCustomerService.resolve(principal);
	}

	private boolean ownsCartItem(User user, CartItem cartItem) {

		if (user == null || cartItem == null) {
			return false;
		}

		ShoppingCart userCart = user.getShoppingCart();
		ShoppingCart itemCart = cartItem.getShoppingCart();

		return userCart != null
				&& itemCart != null
				&& Objects.equals(
						userCart.getId(),
						itemCart.getId());
	}

	/**
	 * VIEW SHOPPING CART
	 */
	@GetMapping("/cart")
	public String shoppingCart(
			Model model,
			Principal principal) {

		User user = getAuthenticatedUser(principal);

		if (user == null) {
			return "redirect:/login";
		}

		ShoppingCart shoppingCart = user.getShoppingCart();

		if (shoppingCart == null) {
			throw new IllegalStateException(
					"Shopping cart is not configured for this customer");
		}

		List<CartItem> cartItemList = cartItemService.findByShoppingCart(shoppingCart);

		shoppingCartService.updateShoppingCart(shoppingCart);

		model.addAttribute("cartItemList", cartItemList);
		model.addAttribute("shoppingCart", shoppingCart);

		return "shoppingCart";
	}

	/**
	 * ADD BREAD TO CART
	 */
	@PostMapping("/addItem")
	public String addItem(
			@RequestParam("breadId") Long breadId,
			@RequestParam("qty") int qty,
			Model model,
			Principal principal) {

		User user = getAuthenticatedUser(principal);

		if (user == null) {
			return "redirect:/login";
		}

		Optional<Bread> breadOptional = breadService.findById(breadId);

		if (breadOptional.isEmpty()) {
			return "redirect:/breadShelf";
		}

		Bread bread = breadOptional.get();

		if (qty < 1) {
			model.addAttribute("invalidQuantity", true);
			return "forward:/breadDetail?id=" + bread.getId();
		}

		if (qty > bread.getInStockNumber()) {
			model.addAttribute("notEnoughStock", true);
			return "forward:/breadDetail?id=" + bread.getId();
		}

		cartItemService.addBreadToCartItem(
				bread,
				user,
				qty);

		model.addAttribute("addBreadSuccess", true);

		return "forward:/breadDetail?id=" + bread.getId();
	}

	/**
	 * UPDATE CART QUANTITY
	 */
	@PostMapping("/updateCartItem")
	public String updateShoppingCart(
			@RequestParam("id") Long cartItemId,
			@RequestParam("qty") int qty,
			Principal principal) {

		User user = getAuthenticatedUser(principal);

		if (user == null) {
			return "redirect:/login";
		}

		CartItem cartItem = cartItemService.findById(cartItemId);

		if (!ownsCartItem(user, cartItem)) {
			throw new AccessDeniedException(
					"You cannot update another customer's cart");
		}

		if (qty < 1) {
			throw new IllegalArgumentException(
					"Quantity must be at least one");
		}

		if (cartItem.getBread() == null
				|| qty > cartItem.getBread().getInStockNumber()) {

			throw new IllegalArgumentException(
					"Requested quantity exceeds available stock");
		}

		cartItem.setQty(qty);
		cartItemService.updateCartItem(cartItem);

		return "redirect:/shoppingCart/cart";
	}

	/**
	 * REMOVE CART ITEM
	 */
	@PostMapping("/removeItem")
	public String removeItem(
			@RequestParam("id") Long cartItemId,
			Principal principal) {

		User user = getAuthenticatedUser(principal);

		if (user == null) {
			return "redirect:/login";
		}

		CartItem cartItem = cartItemService.findById(cartItemId);

		if (!ownsCartItem(user, cartItem)) {
			throw new AccessDeniedException(
					"You cannot remove another customer's cart item");
		}

		cartItemService.removeCartItem(cartItem);

		return "redirect:/shoppingCart/cart";
	}
}
