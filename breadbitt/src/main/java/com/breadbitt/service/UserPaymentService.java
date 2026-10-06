package com.breadbitt.service;

import com.breadbitt.domain.UserPayment;

public interface UserPaymentService {
    UserPayment findById(Long id);
    void removeById(Long id);
}
