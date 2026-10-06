package com.breadbitt.service.impl;

import com.breadbitt.domain.UserShipping;
import com.breadbitt.repository.UserShippingRepository;
import com.breadbitt.service.UserShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserShippingServiceImpl implements UserShippingService {

    @Autowired
    private UserShippingRepository userShippingRepository;

    @Override
    public UserShipping findById(Long id) {
        Optional<UserShipping> userShipping = userShippingRepository.findById(id);
        return userShipping.orElse(null);
    }

    @Override
    public void removeById(Long id) {
        userShippingRepository.deleteById(id);
    }
}
