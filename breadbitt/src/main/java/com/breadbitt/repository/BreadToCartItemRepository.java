package com.breadbitt.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import com.breadbitt.domain.BreadToCartItem;
import com.breadbitt.domain.CartItem;

@Transactional
public interface BreadToCartItemRepository extends CrudRepository<BreadToCartItem, Long> {
	void deleteByCartItem(CartItem cartItem);
}