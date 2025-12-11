package com.marketinghub.emailservice.service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketingHubTemplateResponse(
        String id,
        String name,
        String subject,
        String htmlContent,
        String textContent
) {
}
