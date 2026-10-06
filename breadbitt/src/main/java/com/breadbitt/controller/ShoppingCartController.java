package com.breadbitt.controller;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.breadbitt.domain.Bread;
import com.breadbitt.domain.CartItem;
import com.breadbitt.domain.ShoppingCart;
import com.breadbitt.domain.User;
import com.breadbitt.service.BreadService;
import com.breadbitt.service.CartItemService;
import com.breadbitt.service.ShoppingCartService;
import com.breadbitt.service.UserService;

@Controller
@RequestMapping("/shoppingCart")
public class ShoppingCartController {
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private CartItemService cartItemService;
	
	@Autowired
	private BreadService breadService;
	
	@Autowired
	private ShoppingCartService shoppingCartService;
	
	@RequestMapping("/cart")
	public String shoppingCart(Model model, Principal principal) {
		User user = userService.findByUsername(principal.getName());
		ShoppingCart shoppingCart = user.getShoppingCart();
		
		if (shoppingCart == null) {
            throw new IllegalArgumentException("ShoppingCart is null");
        }
		
		List<CartItem> cartItemList = cartItemService.findByShoppingCart(shoppingCart);
		
		shoppingCartService.updateShoppingCart(shoppingCart);
		
		model.addAttribute("cartItemList", cartItemList);
		model.addAttribute("shoppingCart", shoppingCart);
		
		return "shoppingCart";
	}

	@RequestMapping("/addItem")
	public String addItem(
	        @ModelAttribute("breadId") Long breadId,
	        @ModelAttribute("qty") String qty,
	        Model model, Principal principal
	) {
	    User user = userService.findByUsername(principal.getName());
	    Optional<Bread> breadOptional = breadService.findById(breadId);

	    if (breadOptional.isPresent()) {
	        Bread bread = breadOptional.get();

	        if (Integer.parseInt(qty) > bread.getInStockNumber()) {
	            model.addAttribute("notEnoughStock", true);
	            return "forward:/breadDetail?id=" + bread.getId();
	        }

	        CartItem cartItem = cartItemService.addBreadToCartItem(bread, user, Integer.parseInt(qty));
	        model.addAttribute("addBreadSuccess", true);

	        return "forward:/breadDetail?id=" + bread.getId();
	    } else {
	        // Handle case when bread is not found
	        model.addAttribute("breadNotFound", true);
	        return "redirect:/breadshelf"; // Adjust the redirect as needed
	    }
	}

	@RequestMapping("/updateCartItem")
	public String updateShoppingCart(
			@ModelAttribute("id") Long cartItemId,
			@ModelAttribute("qty") int qty
	) {
		CartItem cartItem = cartItemService.findById(cartItemId);
		cartItem.setQty(qty);
		cartItemService.updateCartItem(cartItem);
		
		return "forward:/shoppingCart/cart";
	}
	
	@RequestMapping("/removeItem")
	public String removeItem(@RequestParam("id") Long id) {
		cartItemService.removeCartItem(cartItemService.findById(id));
		
		return "forward:/shoppingCart/cart";
	}
}
