package com.screenleads.backend.app.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.screenleads.backend.app.domain.model.Customer;
import com.screenleads.backend.app.domain.model.PromotionRedemption;
import com.screenleads.backend.app.domain.model.UserSegment;
import com.screenleads.backend.app.domain.repositories.CustomerRepository;
import com.screenleads.backend.app.domain.repositories.PromotionRedemptionRepository;
import com.screenleads.backend.app.web.dto.CreateCustomerRequest;
import com.screenleads.backend.app.web.dto.CustomerDTO;
import com.screenleads.backend.app.web.dto.CustomerSearchCriteria;
import com.screenleads.backend.app.web.dto.CustomerStatsDTO;
import com.screenleads.backend.app.web.dto.UpdateCustomerRequest;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementación del servicio de gestión de Customers
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerServiceImpl implements CustomerService {
    
    private final CustomerRepository customerRepository;
    private final PromotionRedemptionRepository redemptionRepository;
    private final ObjectMapper objectMapper;
    
    @Override
    public CustomerDTO createCustomer(CreateCustomerRequest request) {
        log.info("Creating new customer with email: {} and phone: {}", request.getEmail(), request.getPhone());
        
        // Validar identificador
        if (!request.hasValidIdentifier()) {
            throw new IllegalArgumentException("Customer must have at least email or phone");
        }
        
        // Validar unicidad
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            customerRepository.findByEmail(request.getEmail())
                .ifPresent(c -> {
                    throw new IllegalStateException("Email already exists: " + request.getEmail());
                });
        }
        
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            customerRepository.findByPhone(request.getPhone())
                .ifPresent(c -> {
                    throw new IllegalStateException("Phone already exists: " + request.getPhone());
                });
        }
        
        // Crear customer
        Customer customer = Customer.builder()
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .birthDate(request.getBirthDate())
            .gender(request.getGender())
            .city(request.getCity())
            .postalCode(request.getPostalCode())
            .country(request.getCountry())
            .preferredLanguage(request.getPreferredLanguage() != null ? request.getPreferredLanguage() : "es")
            .socialProfiles(request.getSocialProfiles())
            .marketingOptIn(request.getMarketingOptIn())
            .dataProcessingConsent(request.getDataProcessingConsent())
            .thirdPartyDataSharing(request.getThirdPartyDataSharing())
            .emailVerified(false)
            .phoneVerified(false)
            .engagementScore(0)
            .segment(UserSegment.COLD)
            .totalRedemptions(0)
            .uniquePromotionsRedeemed(0)
            .lifetimeValue(BigDecimal.ZERO)
            .firstInteractionAt(Instant.now())
            .lastInteractionAt(Instant.now())
            .build();
        
        // Añadir método de autenticación
        customer.addAuthMethod(request.getAuthMethod());
        
        // Guardar timestamps de consentimientos
        Instant now = Instant.now();
        if (Boolean.TRUE.equals(request.getMarketingOptIn())) {
            customer.setMarketingOptInAt(now);
        }
        if (Boolean.TRUE.equals(request.getDataProcessingConsent())) {
            customer.setDataProcessingConsentAt(now);
        }
        if (Boolean.TRUE.equals(request.getThirdPartyDataSharing())) {
            customer.setThirdPartyDataSharingAt(now);
        }
        
        Customer saved = customerRepository.save(customer);
        log.info("Customer created successfully with ID: {}", saved.getId());
        
        return mapToDTO(saved);
    }
    
    @Override
    public CustomerDTO updateCustomer(Long id, UpdateCustomerRequest request) {
        log.info("Updating customer ID: {}", id);
        
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
        
        // Actualizar campos si están presentes
        if (request.getFirstName() != null) {
            customer.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            customer.setLastName(request.getLastName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(customer.getEmail())) {
            // Verificar unicidad
            customerRepository.findByEmail(request.getEmail())
                .ifPresent(c -> {
                    throw new IllegalStateException("Email already exists: " + request.getEmail());
                });
            customer.setEmail(request.getEmail());
            customer.setEmailVerified(false); // Requiere re-verificación
        }
        if (request.getPhone() != null && !request.getPhone().equals(customer.getPhone())) {
            customerRepository.findByPhone(request.getPhone())
                .ifPresent(c -> {
                    throw new IllegalStateException("Phone already exists: " + request.getPhone());
                });
            customer.setPhone(request.getPhone());
            customer.setPhoneVerified(false);
        }
        if (request.getBirthDate() != null) {
            customer.setBirthDate(request.getBirthDate());
        }
        if (request.getGender() != null) {
            customer.setGender(request.getGender());
        }
        if (request.getCity() != null) {
            customer.setCity(request.getCity());
        }
        if (request.getPostalCode() != null) {
            customer.setPostalCode(request.getPostalCode());
        }
        if (request.getCountry() != null) {
            customer.setCountry(request.getCountry());
        }
        if (request.getPreferredLanguage() != null) {
            customer.setPreferredLanguage(request.getPreferredLanguage());
        }
        if (request.getSocialProfiles() != null) {
            customer.setSocialProfiles(request.getSocialProfiles());
        }
        
        // Consentimientos
        Instant now = Instant.now();
        if (request.getMarketingOptIn() != null) {
            customer.setMarketingOptIn(request.getMarketingOptIn());
            if (Boolean.TRUE.equals(request.getMarketingOptIn())) {
                customer.setMarketingOptInAt(now);
            }
        }
        if (request.getDataProcessingConsent() != null) {
            customer.setDataProcessingConsent(request.getDataProcessingConsent());
            if (Boolean.TRUE.equals(request.getDataProcessingConsent())) {
                customer.setDataProcessingConsentAt(now);
            }
        }
        if (request.getThirdPartyDataSharing() != null) {
            customer.setThirdPartyDataSharing(request.getThirdPartyDataSharing());
            if (Boolean.TRUE.equals(request.getThirdPartyDataSharing())) {
                customer.setThirdPartyDataSharingAt(now);
            }
        }
        
        // Segmentación (solo admin)
        if (request.getTags() != null) {
            customer.setTags(request.getTags());
        }
        if (request.getEngagementScore() != null) {
            customer.setEngagementScore(request.getEngagementScore());
        }
        if (request.getSegment() != null) {
            customer.setSegment(request.getSegment());
        }
        
        Customer updated = customerRepository.save(customer);
        log.info("Customer updated successfully: {}", id);
        
        return mapToDTO(updated);
    }
    
    @Override
    @Transactional(readOnly = true)
    public CustomerDTO findById(Long id) {
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
        return mapToDTO(customer);
    }
    
    @Override
    @Transactional(readOnly = true)
    public CustomerDTO findByEmail(String email) {
        return customerRepository.findByEmail(email)
            .map(this::mapToDTO)
            .orElse(null);
    }
    
    @Override
    @Transactional(readOnly = true)
    public CustomerDTO findByPhone(String phone) {
        return customerRepository.findByPhone(phone)
            .map(this::mapToDTO)
            .orElse(null);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<CustomerDTO> searchCustomers(CustomerSearchCriteria criteria, Pageable pageable) {
        log.info("Searching customers with criteria: {}", criteria);
        
        Specification<Customer> spec = (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();
            
            // Búsqueda por texto
            if (criteria.getSearchTerm() != null && !criteria.getSearchTerm().isBlank()) {
                String searchPattern = "%" + criteria.getSearchTerm().toLowerCase() + "%";
                Predicate emailLike = cb.like(cb.lower(root.get("email")), searchPattern);
                Predicate phoneLike = cb.like(cb.lower(root.get("phone")), searchPattern);
                Predicate firstNameLike = cb.like(cb.lower(root.get("firstName")), searchPattern);
                Predicate lastNameLike = cb.like(cb.lower(root.get("lastName")), searchPattern);
                predicates.add(cb.or(emailLike, phoneLike, firstNameLike, lastNameLike));
            }
            
            // Filtros específicos
            if (criteria.getSegment() != null) {
                predicates.add(cb.equal(root.get("segment"), criteria.getSegment()));
            }
            if (criteria.getCity() != null) {
                predicates.add(cb.equal(cb.lower(root.get("city")), criteria.getCity().toLowerCase()));
            }
            if (criteria.getCountry() != null) {
                predicates.add(cb.equal(cb.lower(root.get("country")), criteria.getCountry().toLowerCase()));
            }
            if (criteria.getEmailVerified() != null) {
                predicates.add(cb.equal(root.get("emailVerified"), criteria.getEmailVerified()));
            }
            if (criteria.getPhoneVerified() != null) {
                predicates.add(cb.equal(root.get("phoneVerified"), criteria.getPhoneVerified()));
            }
            if (criteria.getMarketingOptIn() != null) {
                predicates.add(cb.equal(root.get("marketingOptIn"), criteria.getMarketingOptIn()));
            }
            
            // Rango de fechas
            if (criteria.getCreatedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.getCreatedFrom()));
            }
            if (criteria.getCreatedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.getCreatedTo()));
            }
            if (criteria.getLastInteractionFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("lastInteractionAt"), criteria.getLastInteractionFrom()));
            }
            if (criteria.getLastInteractionTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("lastInteractionAt"), criteria.getLastInteractionTo()));
            }
            
            // Métricas
            if (criteria.getMinRedemptions() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalRedemptions"), criteria.getMinRedemptions()));
            }
            if (criteria.getMaxRedemptions() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalRedemptions"), criteria.getMaxRedemptions()));
            }
            if (criteria.getMinEngagementScore() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("engagementScore"), criteria.getMinEngagementScore()));
            }
            if (criteria.getMaxEngagementScore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("engagementScore"), criteria.getMaxEngagementScore()));
            }
            
            // Tags (contains)
            if (criteria.getTag() != null && !criteria.getTag().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("tags")), "%" + criteria.getTag().toLowerCase() + "%"));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        Page<Customer> customers = customerRepository.findAll(spec, pageable);
        List<CustomerDTO> dtos = customers.getContent().stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
        
        return new PageImpl<>(dtos, pageable, customers.getTotalElements());
    }
    
    @Override
    public void deleteCustomer(Long id) {
        log.info("Deleting customer ID: {}", id);
        
        if (!customerRepository.existsById(id)) {
            throw new IllegalArgumentException("Customer not found: " + id);
        }
        
        customerRepository.deleteById(id);
        log.info("Customer deleted successfully: {}", id);
    }
    
    @Override
    public void verifyEmail(Long customerId, String token) {
        log.info("Verifying email for customer ID: {}", customerId);
        
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        
        // TODO: Validar token contra sistema de verificación
        
        customer.setEmailVerified(true);
        customer.setEmailVerifiedAt(Instant.now());
        customerRepository.save(customer);
        
        log.info("Email verified successfully for customer: {}", customerId);
    }
    
    @Override
    public void verifyPhone(Long customerId, String code) {
        log.info("Verifying phone for customer ID: {}", customerId);
        
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        
        // TODO: Validar código contra sistema SMS
        
        customer.setPhoneVerified(true);
        customer.setPhoneVerifiedAt(Instant.now());
        customerRepository.save(customer);
        
        log.info("Phone verified successfully for customer: {}", customerId);
    }
    
    @Override
    public void updateSegment(Long customerId, UserSegment segment) {
        log.info("Updating segment for customer ID: {} to {}", customerId, segment);
        
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        
        customer.setSegment(segment);
        customerRepository.save(customer);
    }
    
    @Override
    public void addTags(Long customerId, Set<String> tags) {
        log.info("Adding tags to customer ID: {}: {}", customerId, tags);
        
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        
        // Parse existing tags
        Set<String> existingTags = parseTags(customer.getTags());
        existingTags.addAll(tags);
        
        // Serialize back to JSON
        customer.setTags(serializeTags(existingTags));
        customerRepository.save(customer);
    }
    
    @Override
    @Transactional(readOnly = true)
    public CustomerStatsDTO getCustomerStats(Long customerId) {
        log.info("Getting stats for customer ID: {}", customerId);
        
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        
        List<PromotionRedemption> redemptions = redemptionRepository.findByCustomerId(customerId);
        
        // Calcular estadísticas
        Instant now = Instant.now();
        Long daysSinceLastInteraction = null;
        if (customer.getLastInteractionAt() != null) {
            daysSinceLastInteraction = ChronoUnit.DAYS.between(customer.getLastInteractionAt(), now);
        }
        
        // Actividad reciente
        Instant last7Days = now.minus(7, ChronoUnit.DAYS);
        Instant last30Days = now.minus(30, ChronoUnit.DAYS);
        Instant last90Days = now.minus(90, ChronoUnit.DAYS);
        
        int redemptionsLast7Days = (int) redemptions.stream()
            .filter(r -> r.getCreatedAt().isAfter(last7Days))
            .count();
        
        int redemptionsLast30Days = (int) redemptions.stream()
            .filter(r -> r.getCreatedAt().isAfter(last30Days))
            .count();
        
        int redemptionsLast90Days = (int) redemptions.stream()
            .filter(r -> r.getCreatedAt().isAfter(last90Days))
            .count();
        
        // Valor promedio
        BigDecimal avgRedemptionValue = BigDecimal.ZERO;
        if (!redemptions.isEmpty()) {
            BigDecimal totalValue = redemptions.stream()
                .map(r -> r.getBillingAmount() != null ? r.getBillingAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            avgRedemptionValue = totalValue.divide(
                BigDecimal.valueOf(redemptions.size()), 
                2, 
                RoundingMode.HALF_UP
            );
        }
        
        // Frecuencia promedio
        Double avgDaysBetweenRedemptions = null;
        if (redemptions.size() > 1 && customer.getFirstInteractionAt() != null) {
            long totalDays = ChronoUnit.DAYS.between(customer.getFirstInteractionAt(), now);
            avgDaysBetweenRedemptions = (double) totalDays / redemptions.size();
        }
        
        return CustomerStatsDTO.builder()
            .customerId(customerId)
            .totalRedemptions(customer.getTotalRedemptions())
            .uniquePromotionsRedeemed(customer.getUniquePromotionsRedeemed())
            .lifetimeValue(customer.getLifetimeValue())
            .engagementScore(customer.getEngagementScore())
            .firstInteractionAt(customer.getFirstInteractionAt())
            .lastInteractionAt(customer.getLastInteractionAt())
            .daysSinceLastInteraction(daysSinceLastInteraction)
            .redemptionsLast7Days(redemptionsLast7Days)
            .redemptionsLast30Days(redemptionsLast30Days)
            .redemptionsLast90Days(redemptionsLast90Days)
            .avgRedemptionValue(avgRedemptionValue)
            .avgDaysBetweenRedemptions(avgDaysBetweenRedemptions)
            .build();
    }
    
    @Override
    public void recalculateEngagementScore(Long customerId) {
        log.info("Recalculating engagement score for customer ID: {}", customerId);
        
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        
        // Algoritmo de engagement score (0-100)
        int score = 0;
        
        // 1. Total de redemptions (máx 40 puntos)
        score += Math.min(customer.getTotalRedemptions() * 5, 40);
        
        // 2. Recencia (máx 30 puntos)
        if (customer.getLastInteractionAt() != null) {
            long daysSinceLastInteraction = ChronoUnit.DAYS.between(customer.getLastInteractionAt(), Instant.now());
            if (daysSinceLastInteraction <= 7) {
                score += 30;
            } else if (daysSinceLastInteraction <= 30) {
                score += 20;
            } else if (daysSinceLastInteraction <= 90) {
                score += 10;
            }
        }
        
        // 3. Verificación (máx 20 puntos)
        if (Boolean.TRUE.equals(customer.getEmailVerified())) {
            score += 10;
        }
        if (Boolean.TRUE.equals(customer.getPhoneVerified())) {
            score += 10;
        }
        
        // 4. Marketing opt-in (máx 10 puntos)
        if (Boolean.TRUE.equals(customer.getMarketingOptIn())) {
            score += 10;
        }
        
        customer.setEngagementScore(Math.min(score, 100));
        customerRepository.save(customer);
        
        log.info("Engagement score updated to {} for customer: {}", score, customerId);
    }
    
    @Override
    public void updateLifetimeValue(Long customerId) {
        log.info("Updating lifetime value for customer ID: {}", customerId);
        
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        
        List<PromotionRedemption> redemptions = redemptionRepository.findByCustomerId(customerId);
        
        BigDecimal lifetimeValue = redemptions.stream()
            .map(r -> r.getBillingAmount() != null ? r.getBillingAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        customer.setLifetimeValue(lifetimeValue);
        customerRepository.save(customer);
        
        log.info("Lifetime value updated to {} for customer: {}", lifetimeValue, customerId);
    }
    
    // ==================== Métodos privados ====================
    
    private CustomerDTO mapToDTO(Customer customer) {
        return new CustomerDTO(
            customer.getId(),
            customer.getFirstName(),
            customer.getLastName(),
            customer.getEmail(),
            customer.getPhone(),
            customer.getBirthDate(),
            customer.getGender(),
            customer.getCity(),
            customer.getPostalCode(),
            customer.getCountry(),
            customer.getPreferredLanguage(),
            customer.getAuthMethods(),
            customer.getSocialProfiles(),
            customer.getMarketingOptIn(),
            customer.getMarketingOptInAt(),
            customer.getDataProcessingConsent(),
            customer.getDataProcessingConsentAt(),
            customer.getThirdPartyDataSharing(),
            customer.getThirdPartyDataSharingAt(),
            customer.getEmailVerified(),
            customer.getEmailVerifiedAt(),
            customer.getPhoneVerified(),
            customer.getPhoneVerifiedAt(),
            customer.getTags(),
            customer.getEngagementScore(),
            customer.getSegment(),
            customer.getTotalRedemptions(),
            customer.getUniquePromotionsRedeemed(),
            customer.getLifetimeValue(),
            customer.getFirstInteractionAt(),
            customer.getLastInteractionAt(),
            customer.getCreatedAt(),
            customer.getUpdatedAt()
        );
    }
    
    private Set<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return new HashSet<>();
        }
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<Set<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error parsing tags JSON: {}", tagsJson, e);
            return new HashSet<>();
        }
    }
    
    private String serializeTags(Set<String> tags) {
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            log.error("Error serializing tags: {}", tags, e);
            return "[]";
        }
    }
}
