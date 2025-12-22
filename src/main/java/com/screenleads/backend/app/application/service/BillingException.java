package com.screenleads.backend.app.application.service;

/**
 * Exception thrown when billing operations fail.
 */
public class BillingException extends Exception {

    public BillingException(String message) {
        super(message);
    }

    public BillingException(String message, Throwable cause) {
        super(message, cause);
    }
}
