package com.example.marketinghub.service;

import com.example.marketinghub.model.Lead;
import com.example.marketinghub.model.SequenceTemplate;

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
