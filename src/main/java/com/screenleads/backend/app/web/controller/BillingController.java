package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.BillingException;
import com.screenleads.backend.app.application.service.StripeBillingService;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {
    private final StripeBillingService billing;
    private final CompanyRepository companies;

    @PostMapping("/checkout-session/{companyId}")
    @PreAuthorize("hasRole('admin') or hasRole('company_admin')")
    public Map<String, String> createCheckout(@PathVariable Long companyId) throws BillingException {
        var company = companies.findById(companyId).orElseThrow();
        String sessionId = billing.createCheckoutSession(company);
        return Map.of("id", sessionId);
    }

    @PostMapping("/portal-session/{companyId}")
    @PreAuthorize("hasRole('admin') or hasRole('company_admin')")
    public Map<String, String> portal(@PathVariable Long companyId) throws BillingException {
        var company = companies.findById(companyId).orElseThrow();
        String url = billing.createBillingPortalSession(company);
        return Map.of("url", url);
    }
}
