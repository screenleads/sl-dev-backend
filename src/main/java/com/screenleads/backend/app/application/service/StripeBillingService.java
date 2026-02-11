package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.Company;

public interface StripeBillingService {
    String ensureCustomer(Company c) throws BillingException;

    String createCheckoutSession(Company c) throws BillingException;

    String createBillingPortalSession(Company c) throws BillingException;

    void reportLeadUsage(Company c, long quantity, long unixTs) throws BillingException;

    Company syncStripeData(Company company) throws BillingException;
}