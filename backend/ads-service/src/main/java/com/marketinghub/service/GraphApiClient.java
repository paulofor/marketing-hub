package com.marketinghub.service;

import com.marketinghub.model.Lead;
import com.marketinghub.model.SequenceTemplate;

/**
 * Client for Facebook Graph API.
 */
public interface GraphApiClient {
    /**
     * Sends welcome message asynchronously.
     *
     * @param lead lead to welcome
     * @param template message sequence template
     */
    void sendWelcomeAsync(Lead lead, SequenceTemplate template);
}
