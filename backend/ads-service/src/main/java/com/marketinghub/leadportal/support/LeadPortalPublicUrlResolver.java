package com.marketinghub.leadportal.support;

import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.integration.LeadPortalIntegrationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Resolves the public URL for a lead portal flow based on the configured integration settings.
 */
@Component
public class LeadPortalPublicUrlResolver {
    private final LeadPortalIntegrationProperties integrationProperties;

    public LeadPortalPublicUrlResolver(LeadPortalIntegrationProperties integrationProperties) {
        this.integrationProperties = integrationProperties;
    }

    /**
     * Builds the public URL for the given flow when the integration is enabled and properly configured.
     *
     * @param flow Selected lead portal flow (may be {@code null}).
     * @return Public URL or {@code null} when the flow cannot be exposed yet.
     */
    public String resolve(LeadPortalFlow flow) {
        if (flow == null || !flow.isApproved()) {
            return null;
        }
        if (!integrationProperties.isEnabled()) {
            return null;
        }
        if (!StringUtils.hasText(integrationProperties.getBaseUrl())) {
            return null;
        }
        return UriComponentsBuilder.fromHttpUrl(integrationProperties.getBaseUrl())
                .path("/flows/{slug}")
                .buildAndExpand(flow.getSlug())
                .toUriString();
    }
}
