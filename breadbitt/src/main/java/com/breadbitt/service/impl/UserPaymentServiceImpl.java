package com.breadbitt.service.impl;

import com.breadbitt.domain.UserPayment;
import com.breadbitt.repository.UserPaymentRepository;
import com.breadbitt.service.UserPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserPaymentServiceImpl implements UserPaymentService {

    @Autowired
    private UserPaymentRepository userPaymentRepository;

    @Override
    public UserPayment findById(Long id) {
        Optional<UserPayment> userPayment = userPaymentRepository.findById(id);
        return userPayment.orElse(null);
    }

    @Override
    public void removeById(Long id) {
        userPaymentRepository.deleteById(id);
    }
}
