package com.marketinghub.journey.execution.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for SendGrid email delivery.
 */
@Component
@ConfigurationProperties(prefix = "integrations.sendgrid")
@Getter
@Setter
@ToString
public class SendGridProperties {
    private boolean enabled = false;
    private String baseUrl = "https://api.sendgrid.com/v3";
    private String apiKey;
    private String fromEmail;
    private String fromName;
}
