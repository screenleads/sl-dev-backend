package com.screenleads.backend.app.application.web;

import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.stripe.net.Webhook;
import com.stripe.model.checkout.Session;
import com.stripe.model.Subscription;
import com.stripe.StripeClient;
import com.stripe.model.Invoice;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {
    @Value("${stripe.webhookSecret}")
    private String webhookSecret;
    private final StripeClient stripe;
    private final CompanyRepository companies;

    @PostMapping("/webhook")
    public ResponseEntity<String> handle(@RequestHeader("Stripe-Signature") String sig,
            @RequestBody String payload) {
        try {
            var event = Webhook.constructEvent(payload, sig, webhookSecret);

            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted(event);
                    break;
                case "customer.subscription.updated":
                case "customer.subscription.created":
                case "customer.subscription.deleted":
                    handleSubscriptionEvent(event);
                    break;
                case "invoice.payment_failed":
                case "invoice.paid":
                    handleInvoiceEvent(event);
                    break;
                default:
                    // Ignorar otros tipos de eventos
                    break;
            }
            return ResponseEntity.ok("success");
        } catch (WebhookProcessingException e) {
            return ResponseEntity.badRequest().body("webhook error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("webhook error");
        }
    }

    private void handleCheckoutSessionCompleted(com.stripe.model.Event event) throws WebhookProcessingException {
        try {
            var session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if (session == null) {
                return;
            }

            String subId = session.getSubscription();
            var sub = Subscription.retrieve(subId);
            var item = sub.getItems().getData().get(0);

            String customerId = session.getCustomer();
            var company = companies.findByStripeCustomerId(customerId).orElse(null);
            if (company != null) {
                company.setStripeSubscriptionId(subId);
                company.setStripeSubscriptionItemId(item.getId());
                company.setBillingStatus(sub.getStatus());
                companies.save(company);
            }
        } catch (Exception e) {
            throw new WebhookProcessingException("Failed to handle checkout session completed", e);
        }
    }

    private void handleSubscriptionEvent(com.stripe.model.Event event) {
        var sub = (Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
        if (sub == null) {
            return;
        }

        String customerId = sub.getCustomer();
        var company = companies.findByStripeCustomerId(customerId).orElse(null);
        if (company != null) {
            company.setStripeSubscriptionId(sub.getId());
            if (sub.getItems() != null && !sub.getItems().getData().isEmpty()) {
                company.setStripeSubscriptionItemId(sub.getItems().getData().get(0).getId());
            }
            company.setBillingStatus(sub.getStatus());
            companies.save(company);
        }
    }

    private void handleInvoiceEvent(com.stripe.model.Event event) {
        var invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
        if (invoice != null) {
            // opcional: sync estado, enviar emails, marcar company en grace period, etc.
        }
    }
}
