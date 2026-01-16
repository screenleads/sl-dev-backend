package com.screenleads.backend.app.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.domain.repositories.CustomerRepository;
import com.screenleads.backend.app.domain.repositories.PromotionLeadRepository;
import com.screenleads.backend.app.domain.repositories.PromotionRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponServiceImpl Unit Tests")
class CouponServiceImplTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private PromotionLeadRepository promotionLeadRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CouponServiceImpl couponService;

    private Promotion testPromotion;
    private Customer testCustomer;
    private PromotionLead testCoupon;

    @BeforeEach
    void setUp() {
        testPromotion = Promotion.builder()
                .id(1L)
                .name("Test Promotion")
                .startAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .endAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .leadLimitType(LeadLimitType.NO_LIMIT)
                .leadIdentifierType(LeadIdentifierType.EMAIL)
                .build();

        testCustomer = Customer.builder()
                .id(1L)
                .identifier("test@example.com")
                .identifierType(LeadIdentifierType.EMAIL)
                .build();

        testCoupon = PromotionLead.builder()
                .id(1L)
                .promotion(testPromotion)
                .customer(testCustomer)
                .couponCode("TEST123")
                .couponStatus(CouponStatus.VALID)
                .expiresAt(testPromotion.getEndAt())
                .build();
    }

    @Test
    @DisplayName("issueCoupon should create valid coupon")
    void whenIssueCoupon_thenCreatesValidCoupon() {
        // Arrange
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(testPromotion));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(promotionLeadRepository.findByCouponCode(anyString())).thenReturn(Optional.empty());
        when(promotionLeadRepository.save(any(PromotionLead.class))).thenReturn(testCoupon);

        // Act
        PromotionLead result = couponService.issueCoupon(1L, 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCouponStatus()).isEqualTo(CouponStatus.VALID);
        verify(promotionLeadRepository, times(1)).save(any(PromotionLead.class));
    }

    @Test
    @DisplayName("issueCoupon should throw exception when promotion not found")
    void whenIssueCouponWithInvalidPromotion_thenThrowsException() {
        // Arrange
        when(promotionRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> couponService.issueCoupon(999L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Promotion not found");
    }

    @Test
    @DisplayName("issueCoupon should throw exception when customer not found")
    void whenIssueCouponWithInvalidCustomer_thenThrowsException() {
        // Arrange
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(testPromotion));
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> couponService.issueCoupon(1L, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    @DisplayName("issueCoupon should throw exception when promotion not started")
    void whenIssueCouponBeforeStart_thenThrowsException() {
        // Arrange
        testPromotion.setStartAt(Instant.now().plus(1, ChronoUnit.DAYS));
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(testPromotion));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        // Act & Assert
        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Promotion not started yet");
    }

    @Test
    @DisplayName("issueCoupon should throw exception when promotion ended")
    void whenIssueCouponAfterEnd_thenThrowsException() {
        // Arrange
        testPromotion.setEndAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(testPromotion));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        // Act & Assert
        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Promotion already ended");
    }

    @Test
    @DisplayName("issueCoupon should enforce ONE_PER_PERSON limit")
    void whenIssueCouponWithOnePerPersonLimit_thenEnforcesLimit() {
        // Arrange
        testPromotion.setLeadLimitType(LeadLimitType.ONE_PER_PERSON);
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(testPromotion));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(promotionLeadRepository.countByPromotionIdAndCustomerId(1L, 1L)).thenReturn(1L);

        // Act & Assert
        assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ONE_PER_PERSON");
    }

    @Test
    @DisplayName("validate should return valid coupon")
    void whenValidateValidCoupon_thenReturnsLead() {
        // Arrange
        when(promotionLeadRepository.findByCouponCode("TEST123")).thenReturn(Optional.of(testCoupon));

        // Act
        PromotionLead result = couponService.validate("TEST123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCouponCode()).isEqualTo("TEST123");
    }

    @Test
    @DisplayName("validate should throw exception for cancelled coupon")
    void whenValidateCancelledCoupon_thenThrowsException() {
        // Arrange
        testCoupon.setCouponStatus(CouponStatus.CANCELLED);
        when(promotionLeadRepository.findByCouponCode("TEST123")).thenReturn(Optional.of(testCoupon));

        // Act & Assert
        assertThatThrownBy(() -> couponService.validate("TEST123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Coupon cancelled");
    }

    @Test
    @DisplayName("validate should throw exception for redeemed coupon")
    void whenValidateRedeemedCoupon_thenThrowsException() {
        // Arrange
        testCoupon.setCouponStatus(CouponStatus.REDEEMED);
        when(promotionLeadRepository.findByCouponCode("TEST123")).thenReturn(Optional.of(testCoupon));

        // Act & Assert
        assertThatThrownBy(() -> couponService.validate("TEST123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already redeemed");
    }

    @Test
    @DisplayName("redeem should mark coupon as redeemed")
    void whenRedeemValidCoupon_thenMarksAsRedeemed() {
        // Arrange
        when(promotionLeadRepository.findByCouponCode("TEST123")).thenReturn(Optional.of(testCoupon));
        when(promotionLeadRepository.save(any(PromotionLead.class))).thenReturn(testCoupon);

        // Act
        PromotionLead result = couponService.redeem("TEST123");

        // Assert
        assertThat(result.getCouponStatus()).isEqualTo(CouponStatus.REDEEMED);
        assertThat(result.getRedeemedAt()).isNotNull();
        verify(promotionLeadRepository, times(1)).save(testCoupon);
    }

    @Test
    @DisplayName("redeem should throw exception for already redeemed coupon")
    void whenRedeemAlreadyRedeemed_thenThrowsException() {
        // Arrange
        testCoupon.setCouponStatus(CouponStatus.REDEEMED);
        when(promotionLeadRepository.findByCouponCode("TEST123")).thenReturn(Optional.of(testCoupon));

        // Act & Assert
        assertThatThrownBy(() -> couponService.redeem("TEST123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already redeemed");
    }

    @Test
    @DisplayName("expire should mark coupon as expired")
    void whenExpireCoupon_thenMarksAsExpired() {
        // Arrange
        when(promotionLeadRepository.findByCouponCode("TEST123")).thenReturn(Optional.of(testCoupon));
        when(promotionLeadRepository.save(any(PromotionLead.class))).thenReturn(testCoupon);

        // Act
        PromotionLead result = couponService.expire("TEST123");

        // Assert
        assertThat(result.getCouponStatus()).isEqualTo(CouponStatus.EXPIRED);
        verify(promotionLeadRepository, times(1)).save(testCoupon);
    }

    @Test
    @DisplayName("expire should throw exception for redeemed coupon")
    void whenExpireRedeemedCoupon_thenThrowsException() {
        // Arrange
        testCoupon.setCouponStatus(CouponStatus.REDEEMED);
        when(promotionLeadRepository.findByCouponCode("TEST123")).thenReturn(Optional.of(testCoupon));

        // Act & Assert
        assertThatThrownBy(() -> couponService.expire("TEST123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot expire a redeemed coupon");
    }
}
