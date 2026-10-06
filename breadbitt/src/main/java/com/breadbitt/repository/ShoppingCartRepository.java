package com.breadbitt.repository;

import org.springframework.data.repository.CrudRepository;

import com.breadbitt.domain.ShoppingCart;

public interface ShoppingCartRepository extends CrudRepository<ShoppingCart, Long> {

}
