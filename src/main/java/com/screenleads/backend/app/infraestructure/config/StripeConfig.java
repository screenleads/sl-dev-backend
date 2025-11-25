package com.screenleads.backend.app.infraestructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class StripeConfig {
    @Value("${stripe.secret}")
    private String secret;

    @Bean
    public com.stripe.StripeClient stripeClient() {
        return new com.stripe.StripeClient(secret);
    }
}