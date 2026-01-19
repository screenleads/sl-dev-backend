package com.screenleads.backend.app.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.screenleads.backend.app.web.dto.CompanyBillingDTO;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementación temporal de CompanyBillingService.
 * TODO: Implementar completamente según requisitos de facturación.
 */
@Service
@Slf4j
public class CompanyBillingServiceImpl implements CompanyBillingService {

    @Override
    public List<CompanyBillingDTO> getAllCompanyBillings() {
        log.warn("getAllCompanyBillings() no implementado - retornando lista vacía");
        return List.of();
    }

    @Override
    public Optional<CompanyBillingDTO> getCompanyBillingById(Long id) {
        log.warn("getCompanyBillingById({}) no implementado - retornando Optional.empty()", id);
        return Optional.empty();
    }

    @Override
    public Optional<CompanyBillingDTO> getCompanyBillingByCompanyId(Long companyId) {
        log.warn("getCompanyBillingByCompanyId({}) no implementado - retornando Optional.empty()", companyId);
        return Optional.empty();
    }

    @Override
    public Optional<CompanyBillingDTO> getCompanyBillingByStripeCustomerId(String stripeCustomerId) {
        log.warn("getCompanyBillingByStripeCustomerId({}) no implementado - retornando Optional.empty()",
                stripeCustomerId);
        return Optional.empty();
    }

    @Override
    public CompanyBillingDTO createCompanyBilling(CompanyBillingDTO dto) {
        log.error("createCompanyBilling() no implementado - lanzando UnsupportedOperationException");
        throw new UnsupportedOperationException("CompanyBillingService.createCompanyBilling() no implementado");
    }

    @Override
    public CompanyBillingDTO updateCompanyBilling(Long id, CompanyBillingDTO dto) {
        log.error("updateCompanyBilling({}) no implementado - lanzando UnsupportedOperationException", id);
        throw new UnsupportedOperationException("CompanyBillingService.updateCompanyBilling() no implementado");
    }

    @Override
    public void deleteCompanyBilling(Long id) {
        log.error("deleteCompanyBilling({}) no implementado - lanzando UnsupportedOperationException", id);
        throw new UnsupportedOperationException("CompanyBillingService.deleteCompanyBilling() no implementado");
    }

    @Override
    public CompanyBillingDTO incrementCurrentPeriodUsage(Long id, Integer leadCount) {
        log.error("incrementCurrentPeriodUsage({}, {}) no implementado - lanzando UnsupportedOperationException", id,
                leadCount);
        throw new UnsupportedOperationException("CompanyBillingService.incrementCurrentPeriodUsage() no implementado");
    }

    @Override
    public CompanyBillingDTO resetCurrentPeriod(Long id) {
        log.error("resetCurrentPeriod({}) no implementado - lanzando UnsupportedOperationException", id);
        throw new UnsupportedOperationException("CompanyBillingService.resetCurrentPeriod() no implementado");
    }

    @Override
    public boolean hasReachedDeviceLimit(Long id) {
        log.warn("hasReachedDeviceLimit({}) no implementado - retornando false", id);
        return false;
    }

    @Override
    public boolean hasReachedPromotionLimit(Long id) {
        log.warn("hasReachedPromotionLimit({}) no implementado - retornando false", id);
        return false;
    }
}
