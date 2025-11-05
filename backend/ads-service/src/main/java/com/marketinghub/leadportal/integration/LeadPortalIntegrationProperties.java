package com.marketinghub.leadportal.integration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for synchronising approved lead portal flows with the public portal application.
 */
@Component
@ConfigurationProperties(prefix = "integrations.lead-portal")
@Getter
@Setter
@ToString
public class LeadPortalIntegrationProperties {
    /**
     * Controls whether the integration with the public lead portal should be triggered.
     */
    private boolean enabled = false;

    /**
     * Base URL of the lead portal application (for example {@code https://portal.example.com}).
     */
    private String baseUrl;
}
