package com.example.marketinghub.service;

import com.example.marketinghub.model.Lead;

/**
 * Client for Facebook Graph API.
 */
public interface GraphApiClient {
    /**
     * Sends welcome message asynchronously.
     *
     * @param lead lead to welcome
     */
    void sendWelcomeAsync(Lead lead);
}
