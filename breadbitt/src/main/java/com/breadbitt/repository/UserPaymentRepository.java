package com.breadbitt.repository;

import org.springframework.data.repository.CrudRepository;

import com.breadbitt.domain.UserPayment;

public interface UserPaymentRepository extends CrudRepository<UserPayment, Long>{

	

}