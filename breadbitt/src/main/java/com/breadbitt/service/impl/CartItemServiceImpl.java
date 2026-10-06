package com.breadbitt.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.breadbitt.domain.Bread;
import com.breadbitt.domain.BreadToCartItem;
import com.breadbitt.domain.CartItem;
import com.breadbitt.domain.Order;
import com.breadbitt.domain.ShoppingCart;
import com.breadbitt.domain.User;
import com.breadbitt.repository.BreadToCartItemRepository;
import com.breadbitt.repository.CartItemRepository;
import com.breadbitt.service.CartItemService;


@Service
public class CartItemServiceImpl implements CartItemService{
	
	@Autowired
	private CartItemRepository cartItemRepository;
	
	@Autowired
	private BreadToCartItemRepository breadToCartItemRepository;
	
	public List<CartItem> findByShoppingCart(ShoppingCart shoppingCart) {
		return cartItemRepository.findByShoppingCart(shoppingCart);
	}
	
	public CartItem updateCartItem(CartItem cartItem) {
	    BigDecimal bigDecimal = new BigDecimal(cartItem.getBread().getOurPrice())
	                                .multiply(new BigDecimal(cartItem.getQty()));
	    
	    bigDecimal = bigDecimal.setScale(2, RoundingMode.HALF_UP);
	    cartItem.setSubtotal(bigDecimal);
	    
	    cartItemRepository.save(cartItem);
	    
	    return cartItem;
	}
	
	public CartItem addBreadToCartItem(Bread bread, User user, int qty) {
		List<CartItem> cartItemList = findByShoppingCart(user.getShoppingCart());
		
		for (CartItem cartItem : cartItemList) {
			if(bread.getId() == cartItem.getBread().getId()) {
				cartItem.setQty(cartItem.getQty()+qty);
				cartItem.setSubtotal(new BigDecimal(bread.getOurPrice()).multiply(new BigDecimal(qty)));
				cartItemRepository.save(cartItem);
				return cartItem;
			}
		}
		
		CartItem cartItem = new CartItem();
		cartItem.setShoppingCart(user.getShoppingCart());
		cartItem.setBread(bread);
		
		cartItem.setQty(qty);
		cartItem.setSubtotal(new BigDecimal(bread.getOurPrice()).multiply(new BigDecimal(qty)));
		cartItem = cartItemRepository.save(cartItem);
		
		BreadToCartItem breadToCartItem = new BreadToCartItem();
		breadToCartItem.setBread(bread);
		breadToCartItem.setCartItem(cartItem);
		breadToCartItemRepository.save(breadToCartItem);
		
		return cartItem;
	}
	
	public CartItem findById(Long id) {
	    Optional<CartItem> optionalCartItem = cartItemRepository.findById(id);
	    return optionalCartItem.orElse(null); // Or handle the case when CartItem is not found
	}

	
	public void removeCartItem(CartItem cartItem) {
		breadToCartItemRepository.deleteByCartItem(cartItem);
		cartItemRepository.delete(cartItem);
	}

	public CartItem save(CartItem cartItem) {
		return cartItemRepository.save(cartItem);
	}
	
	public List<CartItem> findByOrder(Order order) {
		return cartItemRepository.findByOrder(order);
	}

}
