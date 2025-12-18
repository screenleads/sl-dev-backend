package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.StripeBillingService;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingController Tests")
class BillingControllerTest {

    @Mock
    private StripeBillingService billingService;

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private BillingController billingController;

    private Company testCompany;

    @BeforeEach
    void setUp() {
        testCompany = Company.builder()
                .id(1L)
                .name("Test Company")
                .build();
    }

    @Test
    @DisplayName("Debería crear sesión de checkout cuando la compañía existe")
    void createCheckout_WhenCompanyExists_ShouldReturnSessionId() throws Exception {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
        when(billingService.createCheckoutSession(testCompany)).thenReturn("session_123");

        // Act
        Map<String, String> result = billingController.createCheckout(1L);

        // Assert
        assertNotNull(result);
        assertEquals("session_123", result.get("id"));
        verify(companyRepository, times(1)).findById(1L);
        verify(billingService, times(1)).createCheckoutSession(testCompany);
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando la compañía no existe en checkout")
    void createCheckout_WhenCompanyNotExists_ShouldThrowException() throws Exception {
        // Arrange
        when(companyRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(Exception.class, () -> billingController.createCheckout(999L));
        verify(companyRepository, times(1)).findById(999L);
        verify(billingService, never()).createCheckoutSession(any());
    }

    @Test
    @DisplayName("Debería crear sesión de portal cuando la compañía existe")
    void portal_WhenCompanyExists_ShouldReturnPortalUrl() throws Exception {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
        when(billingService.createBillingPortalSession(testCompany))
                .thenReturn("https://billing.stripe.com/portal/session_xyz");

        // Act
        Map<String, String> result = billingController.portal(1L);

        // Assert
        assertNotNull(result);
        assertEquals("https://billing.stripe.com/portal/session_xyz", result.get("url"));
        verify(companyRepository, times(1)).findById(1L);
        verify(billingService, times(1)).createBillingPortalSession(testCompany);
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando la compañía no existe en portal")
    void portal_WhenCompanyNotExists_ShouldThrowException() throws Exception {
        // Arrange
        when(companyRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(Exception.class, () -> billingController.portal(999L));
        verify(companyRepository, times(1)).findById(999L);
        verify(billingService, never()).createBillingPortalSession(any());
    }

    @Test
    @DisplayName("Debería manejar errores del servicio de billing en checkout")
    void createCheckout_WhenBillingServiceFails_ShouldPropagateException() throws Exception {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
        when(billingService.createCheckoutSession(testCompany))
                .thenThrow(new RuntimeException("Stripe API error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> billingController.createCheckout(1L));
        verify(billingService, times(1)).createCheckoutSession(testCompany);
    }
}
