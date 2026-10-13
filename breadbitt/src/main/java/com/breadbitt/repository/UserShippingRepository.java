
package com.breadbitt.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.breadbitt.domain.UserShipping;

public interface UserShippingRepository
        extends CrudRepository<UserShipping, Long> {

    // Retrieve shipping addresses belonging to a specific customer.
    List<UserShipping> findByUser_Id(Long userId);
}
