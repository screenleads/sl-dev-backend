package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
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
            String customerId = ensureCustomer(c);
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .setSuccessUrl(frontendUrl + "/billing/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontendUrl + "/billing/cancel")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(priceId)
                                    .setQuantity(1L)
                                    .build())
                    .build();
            com.stripe.model.checkout.Session session = com.stripe.model.checkout.Session.create(params);
            return session.getId();
        } catch (BillingException e) {
            throw e;
        } catch (Exception e) {
            throw new BillingException("Failed to create checkout session", e);
        }
    }

    // 3) Crear sesión del Billing Portal
    public String createBillingPortalSession(Company c) throws BillingException {
        try {
            com.stripe.param.billingportal.SessionCreateParams params = com.stripe.param.billingportal.SessionCreateParams
                    .builder()
                    .setCustomer(c.getStripeCustomerId())
                    .setReturnUrl(frontendUrl + "/billing")
                    .build();
            com.stripe.model.billingportal.Session portal = com.stripe.model.billingportal.Session.create(params);
            return portal.getUrl();
        } catch (Exception e) {
            throw new BillingException("Failed to create billing portal session", e);
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
}