package com.breadbitt.service;

import com.breadbitt.domain.UserShipping;

public interface UserShippingService {
    UserShipping findById(Long id);
    void removeById(Long id);
}
