package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.stripe.model.Customer;
import com.stripe.model.CustomerSearchResult;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionCollection;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.SubscriptionListParams;
import com.stripe.param.checkout.SessionCreateParams;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StripeBillingServiceImpl implements StripeBillingService {
    private final CompanyRepository companyRepo;

    @Value("${stripe.priceId}")
    private String priceId;

    @Value("${app.frontendUrl}")
    private String frontendUrl;

    // 1) Crear (o recuperar) Customer para una Company
    public String ensureCustomer(Company c) throws BillingException {
        try {
            if (c.getStripeCustomerId() != null)
                return c.getStripeCustomerId();
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setName(c.getName())
                    // .setEmail(c.getEmail()) // Descomenta si tienes email en Company
                    .putMetadata("companyId", String.valueOf(c.getId()))
                    .build();
            Customer customer = Customer.create(params);
            c.setStripeCustomerId(customer.getId());
            companyRepo.save(c);
            return customer.getId();
        } catch (Exception e) {
            throw new BillingException("Failed to ensure Stripe customer", e);
        }
    }

    // 2) Checkout Session para suscripción metered
    public String createCheckoutSession(Company c) throws BillingException {
        try {
            // Asegurar que existe el customer (sincronizar si no existe)
            if (c.getStripeCustomerId() == null) {
                c = syncStripeData(c);
            }
            
            String customerId = c.getStripeCustomerId();
            if (customerId == null) {
                throw new BillingException("No se pudo obtener el Customer ID de Stripe. Intenta sincronizar primero.");
            }
            
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .setSuccessUrl(frontendUrl + "/companies?checkout=success")
                    .setCancelUrl(frontendUrl + "/companies?checkout=cancel")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(priceId)
                                    .build())
                    .build();
            com.stripe.model.checkout.Session session = com.stripe.model.checkout.Session.create(params);
            return session.getUrl();  // Retornar URL directamente en lugar de session ID
        } catch (BillingException e) {
            throw e;
        } catch (Exception e) {
            throw new BillingException("Error al crear sesión de checkout: " + e.getMessage(), e);
        }
    }

    // 3) Crear sesión del Billing Portal
    public String createBillingPortalSession(Company c) throws BillingException {
        try {
            if (c.getStripeCustomerId() == null) {
                throw new BillingException("La empresa no tiene un Customer ID de Stripe. Sincroniza primero con Stripe.");
            }
            
            com.stripe.param.billingportal.SessionCreateParams params = com.stripe.param.billingportal.SessionCreateParams
                    .builder()
                    .setCustomer(c.getStripeCustomerId())
                    .setReturnUrl(frontendUrl + "/companies")
                    .build();
            com.stripe.model.billingportal.Session portal = com.stripe.model.billingportal.Session.create(params);
            return portal.getUrl();
        } catch (BillingException e) {
            throw e;
        } catch (Exception e) {
            throw new BillingException("Error al crear sesión del portal de facturación: " + e.getMessage(), e);
        }
    }

    // 4) Reportar uso (incrementar nº de leads) usando llamada HTTP estándar
    public void reportLeadUsage(Company c, long quantity, long unixTs) throws BillingException {
        try {
            if (c.getStripeSubscriptionItemId() == null)
                return;
            String apiKey = System.getenv("STRIPE_SECRET_KEY");
            if (apiKey == null)
                throw new IllegalStateException("STRIPE_SECRET_KEY no configurada");
            String endpoint = String.format("https://api.stripe.com/v1/subscription_items/%s/usage_records",
                    c.getStripeSubscriptionItemId());

            StringBuilder postData = new StringBuilder();
            postData.append("quantity=").append(URLEncoder.encode(String.valueOf(quantity), StandardCharsets.UTF_8));
            postData.append("&timestamp=").append(URLEncoder.encode(String.valueOf(unixTs), StandardCharsets.UTF_8));
            postData.append("&action=increment");

            byte[] postDataBytes = postData.toString().getBytes(StandardCharsets.UTF_8);

            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(postDataBytes);
            }
            int responseCode = conn.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new RuntimeException("Stripe usage report failed: HTTP " + responseCode);
            }
        } catch (Exception e) {
            throw new BillingException("Failed to report lead usage", e);
        }
    }

    // 5) Sincronizar datos de Stripe al Company
    @Override
    public Company syncStripeData(Company company) throws BillingException {
        try {
            Customer customer;
            
            // 1. Buscar customer en Stripe por nombre de empresa
            CustomerSearchParams searchParams = CustomerSearchParams.builder()
                    .setQuery(String.format("name:'%s'", company.getName().replace("'", "\\'")))
                    .build();
            
            CustomerSearchResult customers = Customer.search(searchParams);
            
            if (customers.getData().isEmpty()) {
                // No existe el customer, crearlo automáticamente
                CustomerCreateParams createParams = CustomerCreateParams.builder()
                        .setName(company.getName())
                        .setDescription("Customer creado automáticamente desde ScreenLeads Dashboard")
                        .putMetadata("companyId", String.valueOf(company.getId()))
                        .build();
                
                customer = Customer.create(createParams);
            } else {
                // Tomar el primer resultado
                customer = customers.getData().get(0);
            }
            
            company.setStripeCustomerId(customer.getId());
            
            // 2. Buscar subscripción activa del customer
            SubscriptionListParams subParams = SubscriptionListParams.builder()
                    .setCustomer(customer.getId())
                    .setStatus(SubscriptionListParams.Status.ACTIVE)
                    .setLimit(1L)
                    .build();
            
            SubscriptionCollection subscriptions = Subscription.list(subParams);
            
            if (!subscriptions.getData().isEmpty()) {
                Subscription subscription = subscriptions.getData().get(0);
                company.setStripeSubscriptionId(subscription.getId());
                company.setBillingStatus(Company.BillingStatus.ACTIVE.name());
                
                // 3. Obtener subscription item ID (para usage-based billing)
                if (!subscription.getItems().getData().isEmpty()) {
                    String subscriptionItemId = subscription.getItems().getData().get(0).getId();
                    company.setStripeSubscriptionItemId(subscriptionItemId);
                }
            } else {
                // Customer existe pero no tiene subscripción activa
                company.setStripeSubscriptionId(null);
                company.setStripeSubscriptionItemId(null);
                company.setBillingStatus(Company.BillingStatus.INCOMPLETE.name());
            }
            
            // 4. Guardar y retornar
            return companyRepo.save(company);
            
        } catch (Exception e) {
            throw new BillingException("Error al sincronizar datos de Stripe: " + e.getMessage(), e);
        }
    }
}