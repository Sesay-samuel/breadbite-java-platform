package com.breadbitt.service;

import com.breadbitt.domain.Payment;
import com.breadbitt.domain.UserPayment;

public interface PaymentService {
	Payment setByUserPayment(UserPayment userPayment, Payment payment);
}
