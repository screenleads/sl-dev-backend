package com.screenleads.backend.app.application.service;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.screenleads.backend.app.domain.model.UserSegment;
import com.screenleads.backend.app.web.dto.CreateCustomerRequest;
import com.screenleads.backend.app.web.dto.CustomerDTO;
import com.screenleads.backend.app.web.dto.CustomerSearchCriteria;
import com.screenleads.backend.app.web.dto.CustomerStatsDTO;
import com.screenleads.backend.app.web.dto.UpdateCustomerRequest;

/**
 * Servicio para gestionar clientes/consumidores finales que canjean promociones
 */
public interface CustomerService {

    /**
     * Crea un nuevo customer
     * 
     * @param request datos del customer
     * @return Customer creado
     * @throws IllegalArgumentException si no tiene email ni phone
     * @throws IllegalStateException    si email/phone ya existen
     */
    CustomerDTO createCustomer(CreateCustomerRequest request);

    /**
     * Actualiza un customer existente
     * 
     * @param id      ID del customer
     * @param request datos a actualizar
     * @return Customer actualizado
     * @throws IllegalArgumentException si el customer no existe
     */
    CustomerDTO updateCustomer(Long id, UpdateCustomerRequest request);

    /**
     * Busca customer por ID
     * 
     * @param id ID del customer
     * @return Customer encontrado
     * @throws IllegalArgumentException si no existe
     */
    CustomerDTO findById(Long id);

    /**
     * Busca customer por email
     * 
     * @param email email del customer
     * @return Customer encontrado o null
     */
    CustomerDTO findByEmail(String email);

    /**
     * Busca customer por teléfono
     * 
     * @param phone teléfono del customer
     * @return Customer encontrado o null
     */
    CustomerDTO findByPhone(String phone);

    /**
     * Busca customers con criterios y paginación
     * 
     * @param criteria criterios de búsqueda
     * @param pageable configuración de paginación
     * @return Página de customers
     */
    Page<CustomerDTO> searchCustomers(CustomerSearchCriteria criteria, Pageable pageable);

    /**
     * Lista todos los customers
     * 
     * @return Lista de todos los customers
     */
    List<CustomerDTO> findAll();

    /**
     * Elimina un customer (soft delete)
     * 
     * @param id ID del customer
     */
    void deleteCustomer(Long id);

    /**
     * Verifica el email de un customer
     * 
     * @param customerId ID del customer
     * @param token      token de verificación
     */
    void verifyEmail(Long customerId, String token);

    /**
     * Verifica el teléfono de un customer
     * 
     * @param customerId ID del customer
     * @param code       código de verificación SMS
     */
    void verifyPhone(Long customerId, String code);

    /**
     * Actualiza el segmento de un customer
     * 
     * @param customerId ID del customer
     * @param segment    nuevo segmento
     */
    void updateSegment(Long customerId, UserSegment segment);

    /**
     * Añade tags a un customer
     * 
     * @param customerId ID del customer
     * @param tags       tags a añadir
     */
    void addTags(Long customerId, Set<String> tags);

    /**
     * Obtiene estadísticas de un customer
     * 
     * @param customerId ID del customer
     * @return Estadísticas calculadas
     */
    CustomerStatsDTO getCustomerStats(Long customerId);

    /**
     * Recalcula el engagement score de un customer
     * Basado en: totalRedemptions, daysSinceLastInteraction, conversionRate
     * 
     * @param customerId ID del customer
     */
    void recalculateEngagementScore(Long customerId);

    /**
     * Actualiza el lifetime value de un customer
     * Suma de billingAmount de todos sus redemptions
     * 
     * @param customerId ID del customer
     */
    void updateLifetimeValue(Long customerId);

    /**
     * Obtiene todos los customers que han canjeado promociones de una compañía
     * La relación es: Customer -> PromotionRedemption -> Promotion -> Company
     * 
     * @param companyId ID de la compañía
     * @return Lista de customers únicos que han canjeado promociones de la compañía
     */
    List<CustomerDTO> findCustomersByCompany(Long companyId);
}
