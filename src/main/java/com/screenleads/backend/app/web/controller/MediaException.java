package com.screenleads.backend.app.web.controller;

/**
 * Exception thrown when media operations fail.
 */
public class MediaException extends Exception {

    public MediaException(String message) {
        super(message);
    }

    public MediaException(String message, Throwable cause) {
        super(message, cause);
    }
}
