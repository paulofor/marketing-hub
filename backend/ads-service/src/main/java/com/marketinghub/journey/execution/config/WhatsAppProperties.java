package com.marketinghub.journey.execution.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for Meta WhatsApp Cloud API.
 */
@Component
@ConfigurationProperties(prefix = "integrations.whatsapp")
@Getter
@Setter
@ToString
public class WhatsAppProperties {
    private boolean enabled = false;
    private String baseUrl = "https://graph.facebook.com/v18.0";
    private String accessToken;
    private String phoneNumberId;
}
