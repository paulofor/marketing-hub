package com.marketinghub.emailservice.leadportal.service;

import com.marketinghub.emailservice.config.MarketingHubClientProperties;
import java.net.URI;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Constrói os links públicos usados para rastrear aberturas, visualizações e acesso ao checkout.
 */
@Component
public class LeadPortalTrackingLinkService {

    private final String configuredBaseUrl;
    private final MarketingHubClientProperties marketingHubClientProperties;

    public LeadPortalTrackingLinkService(
            @Value("${lead-portal.notifications.tracking-base-url:}") String configuredBaseUrl,
            MarketingHubClientProperties marketingHubClientProperties) {
        this.configuredBaseUrl = configuredBaseUrl;
        this.marketingHubClientProperties = marketingHubClientProperties;
    }

    public Optional<String> buildPreviewUrl(long packageId, String submissionId) {
        return buildPackageLink(packageId, submissionId, "previews");
    }

    public Optional<String> buildPixelUrl(long packageId, String submissionId) {
        return buildPackageLink(packageId, submissionId, "open.gif");
    }

    public Optional<String> buildCheckoutTrackingUrl(Long purchaseId, String submissionId) {
        if (purchaseId == null) {
            return Optional.empty();
        }
        String baseUrl = resolveBaseUrl();
        if (baseUrl == null) {
            return Optional.empty();
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/api/public/lead-portal/purchases/")
                .path(String.valueOf(purchaseId))
                .path("/checkout");
        if (StringUtils.hasText(submissionId)) {
            builder.queryParam("sid", submissionId.trim());
        }
        return Optional.of(builder.toUriString());
    }

    public Optional<String> trackingHost() {
        String baseUrl = resolveBaseUrl();
        if (baseUrl == null) {
            return Optional.empty();
        }
        try {
            URI uri = URI.create(baseUrl);
            return Optional.ofNullable(uri.getHost());
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private Optional<String> buildPackageLink(long packageId, String submissionId, String suffix) {
        String baseUrl = resolveBaseUrl();
        if (baseUrl == null) {
            return Optional.empty();
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/api/public/lead-portal/image-packages/")
                .path(String.valueOf(packageId))
                .path("/")
                .path(suffix);
        if (StringUtils.hasText(submissionId)) {
            builder.queryParam("sid", submissionId.trim());
        }
        return Optional.of(builder.toUriString());
    }

    private String resolveBaseUrl() {
        if (StringUtils.hasText(configuredBaseUrl)) {
            return normalize(configuredBaseUrl.trim());
        }
        if (marketingHubClientProperties != null && StringUtils.hasText(marketingHubClientProperties.baseUrl())) {
            return normalize(marketingHubClientProperties.baseUrl().trim());
        }
        return null;
    }

    private String normalize(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
