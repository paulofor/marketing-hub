package com.example.marketinghub.service;

import com.example.marketinghub.model.Lead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Trivial implementation that logs the welcome message.
 */
@Service
public class GraphApiClientImpl implements GraphApiClient {
    private static final Logger log = LoggerFactory.getLogger(GraphApiClientImpl.class);

    @Override
    @Async("taskExecutor")
    public void sendWelcomeAsync(Lead lead) {
        log.info("Sending welcome to lead {}", lead.getId());
    }
}
