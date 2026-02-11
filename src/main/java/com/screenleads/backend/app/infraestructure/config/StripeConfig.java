package com.screenleads.backend.app.infraestructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;

@Configuration
public class StripeConfig {
    @Value("${stripe.secret}")
    private String secret;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secret;
    }

    @Bean
    public com.stripe.StripeClient stripeClient() {
        return new com.stripe.StripeClient(secret);
    }
}