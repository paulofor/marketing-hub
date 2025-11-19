package com.marketinghub.journey.model;

/**
 * Types of stimuli supported by the orchestration engine.
 */
public enum JourneyStimulusType {
    AD,
    EMAIL,
    WHATSAPP,
    LANDING_PAGE,
    INSTANT_FORM,
    /**
     * Flow executed inside the lead portal where we collect/submit an image provided by the user.
     */
    LEAD_PORTAL_IMAGE_FLOW,
    /**
     * Static showcase composed of curated images meant to highlight offers inside the lead portal.
     */
    SHOWCASE_IMAGE,
    /**
     * Checkout/payment experience hosted outside of ads that we still track as part of the journey.
     */
    PAYMENT_PAGE
}
