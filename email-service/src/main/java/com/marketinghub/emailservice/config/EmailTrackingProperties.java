package com.marketinghub.emailservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "email.tracking")
public record EmailTrackingProperties(
        String baseUrl
) {
}
