
package com.breadbitt.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.breadbitt.domain.UserPayment;

public interface UserPaymentRepository
        extends CrudRepository<UserPayment, Long> {

    // Retrieve payment methods belonging to a specific customer.
    List<UserPayment> findByUser_Id(Long userId);
}
