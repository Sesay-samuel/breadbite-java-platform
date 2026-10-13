package com.breadbitt.service.impl;

import java.time.Instant;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.breadbitt.domain.BillingAddress;
import com.breadbitt.domain.Bread;
import com.breadbitt.domain.CartItem;
import com.breadbitt.domain.Order;
import com.breadbitt.domain.Payment;
import com.breadbitt.domain.ShippingAddress;
import com.breadbitt.domain.ShoppingCart;
import com.breadbitt.domain.User;
import com.breadbitt.events.OrderPlacedEvent;
import com.breadbitt.repository.OrderRepository;
import com.breadbitt.service.CartItemService;
import com.breadbitt.service.OrderService;

@Service
public class OrderServiceImpl implements OrderService {

	private final OrderRepository orderRepository;
	private final CartItemService cartItemService;
	private final ApplicationEventPublisher eventPublisher;

	public OrderServiceImpl(
			OrderRepository orderRepository,
			CartItemService cartItemService,
			ApplicationEventPublisher eventPublisher) {
		this.orderRepository = orderRepository;
		this.cartItemService = cartItemService;
		this.eventPublisher = eventPublisher;
	}

	@Override
	@Transactional
	public Order createOrder(
			ShoppingCart shoppingCart,
			ShippingAddress shippingAddress,
			BillingAddress billingAddress,
			Payment payment,
			String shippingMethod,
			User user) {

		if (user == null || !user.isEnabled() || user.getId() == null) {
			throw new AccessDeniedException("An active customer account is required");
		}

		if (shoppingCart == null || shoppingCart.getId() == null
				|| user.getShoppingCart() == null
				|| !Objects.equals(user.getShoppingCart().getId(), shoppingCart.getId())) {
			throw new AccessDeniedException("Shopping cart does not belong to this customer");
		}

		if (!"groundShipping".equals(shippingMethod)
				&& !"premiumShipping".equals(shippingMethod)) {
			throw new IllegalArgumentException("Unsupported shipping method");
		}

		if (shippingAddress == null || billingAddress == null || payment == null) {
			throw new IllegalArgumentException("Checkout information is required");
		}

		List<CartItem> cartItemList = cartItemService.findByShoppingCart(shoppingCart);
		if (cartItemList == null || cartItemList.isEmpty()) {
			throw new IllegalStateException("Cannot create an order with an empty shopping cart");
		}

		// Validate every line before changing inventory or persisting an order.
		for (CartItem cartItem : cartItemList) {
			if (cartItem == null || cartItem.getShoppingCart() == null
					|| !Objects.equals(cartItem.getShoppingCart().getId(), shoppingCart.getId())) {
				throw new AccessDeniedException("Cart item does not belong to the customer's cart");
			}
			Bread bread = cartItem.getBread();
			if (bread == null || cartItem.getQty() <= 0) {
				throw new IllegalStateException("Invalid item in shopping cart");
			}
			if (bread.getInStockNumber() < cartItem.getQty()) {
				throw new IllegalStateException("Insufficient stock for a bread product");
			}
		}

		Order order = new Order();
		order.setBillingAddress(billingAddress);
		order.setOrderStatus("created");
		order.setPayment(payment);
		order.setShippingAddress(shippingAddress);
		order.setShippingMethod(shippingMethod);

		for (CartItem cartItem : cartItemList) {
			Bread bread = cartItem.getBread();
			cartItem.setOrder(order);
			bread.setInStockNumber(bread.getInStockNumber() - cartItem.getQty());
		}

		order.setCartItemList(cartItemList);
		order.setOrderDate(Calendar.getInstance().getTime());
		// Existing total calculation is retained pending review of Bread/CartItem
		// pricing.
		// Do not treat this as a verified server-side price recalculation.
		order.setOrderTotal(shoppingCart.getGrandTotal());
		shippingAddress.setOrder(order);
		billingAddress.setOrder(order);
		payment.setOrder(order);
		order.setUser(user);

		Order savedOrder = orderRepository.save(order);
		OrderPlacedEvent event = new OrderPlacedEvent(
				UUID.randomUUID().toString(),
				savedOrder.getId(),
				savedOrder.getOrderTotal(),
				savedOrder.getOrderStatus(),
				Instant.now());
		// Kafka listener processes the event AFTER_COMMIT.
		eventPublisher.publishEvent(event);
		return savedOrder;
	}

	@Override
	@Transactional(readOnly = true)
	public Order findOne(Long id) {
		Optional<Order> order = orderRepository.findById(id);
		return order.orElse(null);
	}
}
