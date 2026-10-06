package com.breadbitt.repository;

import org.springframework.data.repository.CrudRepository;

import com.breadbitt.domain.Order;

public interface OrderRepository extends CrudRepository<Order, Long>{

}
