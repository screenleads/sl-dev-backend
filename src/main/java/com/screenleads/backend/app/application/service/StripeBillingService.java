package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.Company;

public interface StripeBillingService {
    String ensureCustomer(Company c) throws Exception;

    String createCheckoutSession(Company c) throws Exception;

    String createBillingPortalSession(Company c) throws Exception;

    void reportLeadUsage(Company c, long quantity, long unixTs) throws Exception;
}