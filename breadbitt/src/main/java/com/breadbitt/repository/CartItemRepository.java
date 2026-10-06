package com.breadbitt.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.breadbitt.domain.CartItem;
import com.breadbitt.domain.Order;
import com.breadbitt.domain.ShoppingCart;

import jakarta.transaction.Transactional;

@Transactional
public interface CartItemRepository extends CrudRepository<CartItem, Long>{
    List<CartItem> findByShoppingCart(ShoppingCart shoppingCart);
    Optional<CartItem> findById(Long id);
    
    List<CartItem> findByOrder(Order order);
}
