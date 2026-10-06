package com.breadbitt.service;

import com.breadbitt.domain.BillingAddress;
import com.breadbitt.domain.UserBilling;

public interface BillingAddressService {
	BillingAddress setByUserBilling(UserBilling userBilling, BillingAddress billingAddress);
}
