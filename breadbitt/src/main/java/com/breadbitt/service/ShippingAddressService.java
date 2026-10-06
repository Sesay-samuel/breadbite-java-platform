package com.breadbitt.service;

import com.breadbitt.domain.ShippingAddress;
import com.breadbitt.domain.UserShipping;

public interface ShippingAddressService {
	ShippingAddress setByUserShipping(UserShipping userShipping, ShippingAddress shippingAddress);
}

