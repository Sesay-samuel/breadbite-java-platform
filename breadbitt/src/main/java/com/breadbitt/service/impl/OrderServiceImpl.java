
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

		// 1. Validate customer.
		if (user == null
				|| !user.isEnabled()
				|| user.getId() == null) {

			throw new AccessDeniedException(
					"An active customer account is required");
		}

		// 2. Verify cart ownership.
		if (shoppingCart == null
				|| shoppingCart.getId() == null
				|| user.getShoppingCart() == null
				|| !Objects.equals(
						user.getShoppingCart().getId(),
						shoppingCart.getId())) {

			throw new AccessDeniedException(
					"Shopping cart does not belong to this customer");
		}

		// 3. Validate shipping method.
		if (!"groundShipping".equals(shippingMethod)
				&& !"premiumShipping".equals(shippingMethod)) {

			throw new IllegalArgumentException(
					"Unsupported shipping method");
		}

		// 4. Validate checkout objects.
		if (shippingAddress == null
				|| billingAddress == null
				|| payment == null) {

			throw new IllegalArgumentException(
					"Checkout information is required");
		}

		// 5. Retrieve shopping cart items.
		List<CartItem> cartItemList = cartItemService.findByShoppingCart(shoppingCart);

		if (cartItemList == null || cartItemList.isEmpty()) {
			throw new IllegalStateException(
					"Cannot create an order with an empty shopping cart");
		}

		// 6. Validate items and inventory.
		for (CartItem cartItem : cartItemList) {

			if (cartItem == null
					|| cartItem.getShoppingCart() == null
					|| !Objects.equals(
							cartItem.getShoppingCart().getId(),
							shoppingCart.getId())) {

				throw new AccessDeniedException(
						"Cart item does not belong to the customer's cart");
			}

			Bread bread = cartItem.getBread();

			if (bread == null || cartItem.getQty() <= 0) {
				throw new IllegalStateException(
						"Invalid item in shopping cart");
			}

			if (bread.getInStockNumber() < cartItem.getQty()) {
				throw new IllegalStateException(
						"Insufficient stock for a bread product");
			}
		}

		// 7. Create a fresh shipping address for this order.
		ShippingAddress orderShippingAddress = new ShippingAddress();

		orderShippingAddress.setShippingAddressName(
				shippingAddress.getShippingAddressName());

		orderShippingAddress.setShippingAddressStreet1(
				shippingAddress.getShippingAddressStreet1());

		orderShippingAddress.setShippingAddressStreet2(
				shippingAddress.getShippingAddressStreet2());

		orderShippingAddress.setShippingAddressCity(
				shippingAddress.getShippingAddressCity());

		orderShippingAddress.setShippingAddressState(
				shippingAddress.getShippingAddressState());

		orderShippingAddress.setShippingAddressCountry(
				shippingAddress.getShippingAddressCountry());

		orderShippingAddress.setShippingAddressZipcode(
				shippingAddress.getShippingAddressZipcode());

		// 8. Create a fresh billing address for this order.
		BillingAddress orderBillingAddress = new BillingAddress();

		orderBillingAddress.setBillingAddressName(
				billingAddress.getBillingAddressName());

		orderBillingAddress.setBillingAddressStreet1(
				billingAddress.getBillingAddressStreet1());

		orderBillingAddress.setBillingAddressStreet2(
				billingAddress.getBillingAddressStreet2());

		orderBillingAddress.setBillingAddressCity(
				billingAddress.getBillingAddressCity());

		orderBillingAddress.setBillingAddressState(
				billingAddress.getBillingAddressState());

		orderBillingAddress.setBillingAddressCountry(
				billingAddress.getBillingAddressCountry());

		orderBillingAddress.setBillingAddressZipcode(
				billingAddress.getBillingAddressZipcode());

		// 9. Create order-specific payment metadata.
		//
		// Never copy cardNumber or CVC into this order.
		// No existing Payment ID or UserBilling is reused.
		Payment orderPayment = new Payment();

		orderPayment.setType(payment.getType());
		orderPayment.setCardName(payment.getCardName());
		orderPayment.setHolderName(payment.getHolderName());

		// 10. Create order and assign new entities.
		Order order = new Order();

		order.setOrderStatus("created");
		order.setShippingMethod(shippingMethod);

		order.setShippingAddress(orderShippingAddress);
		order.setBillingAddress(orderBillingAddress);
		order.setPayment(orderPayment);

		// 11. Associate items and adjust stock.
		for (CartItem cartItem : cartItemList) {

			Bread bread = cartItem.getBread();

			cartItem.setOrder(order);

			bread.setInStockNumber(
					bread.getInStockNumber() - cartItem.getQty());
		}

		order.setCartItemList(cartItemList);
		order.setOrderDate(Calendar.getInstance().getTime());

		// Retain existing cart total pending pricing review.
		order.setOrderTotal(shoppingCart.getGrandTotal());

		// 12. Set reverse relationships.
		orderShippingAddress.setOrder(order);
		orderBillingAddress.setOrder(order);
		orderPayment.setOrder(order);

		order.setUser(user);

		// 13. Save order to PostgreSQL.
		Order savedOrder = orderRepository.save(order);

		// 14. Publish order event.
		OrderPlacedEvent event = new OrderPlacedEvent(
				UUID.randomUUID().toString(),
				savedOrder.getId(),
				savedOrder.getOrderTotal(),
				savedOrder.getOrderStatus(),
				Instant.now());

		// Kafka event listener runs after transaction commit.
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
