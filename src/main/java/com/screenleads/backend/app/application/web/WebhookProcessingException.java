package com.screenleads.backend.app.application.web;

/**
 * Exception thrown when webhook processing fails.
 */
public class WebhookProcessingException extends Exception {

    public WebhookProcessingException(String message) {
        super(message);
    }

    public WebhookProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
