package com.marketinghub.leadportal.integration;

/**
 * Signals a failure to synchronise a lead portal flow with the public lead portal application.
 */
public class LeadPortalPublicationException extends RuntimeException {

    public LeadPortalPublicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public LeadPortalPublicationException(String message) {
        super(message);
    }
}
