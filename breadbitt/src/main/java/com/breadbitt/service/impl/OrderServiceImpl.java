
package com.breadbitt.service.impl;

import java.time.Instant;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
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

		Order order = new Order();

		order.setBillingAddress(billingAddress);
		order.setOrderStatus("created");
		order.setPayment(payment);
		order.setShippingAddress(shippingAddress);
		order.setShippingMethod(shippingMethod);

		List<CartItem> cartItemList = cartItemService.findByShoppingCart(shoppingCart);

		if (cartItemList == null || cartItemList.isEmpty()) {
			throw new IllegalStateException(
					"Cannot create an order with an empty shopping cart");
		}

		for (CartItem cartItem : cartItemList) {

			Bread bread = cartItem.getBread();

			if (bread == null || cartItem.getQty() <= 0) {
				throw new IllegalStateException(
						"Invalid item in shopping cart");
			}

			if (bread.getInStockNumber() < cartItem.getQty()) {
				throw new IllegalStateException(
						"Insufficient stock for a bread product");
			}

			cartItem.setOrder(order);

			bread.setInStockNumber(
					bread.getInStockNumber() - cartItem.getQty());
		}

		order.setCartItemList(cartItemList);
		order.setOrderDate(Calendar.getInstance().getTime());
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

		// Publish a Spring application event inside the transaction.
		// The Kafka handler will run only AFTER_COMMIT.
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