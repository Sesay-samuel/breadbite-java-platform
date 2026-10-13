package com.breadbitt.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.breadbitt.domain.Order;
import com.breadbitt.domain.User;
import com.breadbitt.domain.UserPayment;
import com.breadbitt.domain.UserShipping;
import com.breadbitt.service.UserService;

@Service
public class ProfileViewService {

    private final UserService userService;

    public ProfileViewService(UserService userService) {
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public ProfileData loadProfile(String username) {

        User user = userService.findByUsername(username);

        if (user == null) {
            return null;
        }

        // Copy collections while the Hibernate session is open.
        List<UserPayment> payments = new ArrayList<>(user.getUserPaymentList());

        List<UserShipping> shipping = new ArrayList<>(user.getUserShippingList());

        List<Order> orders = new ArrayList<>(user.getOrderList());

        return new ProfileData(user, payments, shipping, orders);
    }

    public record ProfileData(
            User user,
            List<UserPayment> payments,
            List<UserShipping> shipping,
            List<Order> orders) {
    }
}