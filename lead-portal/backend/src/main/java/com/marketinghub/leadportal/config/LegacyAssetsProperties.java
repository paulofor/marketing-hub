package com.marketinghub.leadportal.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "lead-portal.legacy-assets")
public class LegacyAssetsProperties {

    /** Base URL where legacy uploads are publicly available. */
    private String baseUrl = "http://191.252.181.168:8000";

    /** Path prefix used by legacy uploads. */
    private String uploadPathPrefix = "/uploads/";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getUploadPathPrefix() {
        return uploadPathPrefix;
    }

    public void setUploadPathPrefix(String uploadPathPrefix) {
        this.uploadPathPrefix = uploadPathPrefix;
    }

    public String normalizedBaseUrl() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public String normalizedUploadPathPrefix() {
        if (uploadPathPrefix == null || uploadPathPrefix.isBlank()) {
            return "/uploads/";
        }
        String prefix = uploadPathPrefix.startsWith("/") ? uploadPathPrefix : "/" + uploadPathPrefix;
        return prefix.endsWith("/") ? prefix : prefix + "/";
    }

    public URI resolveUploadUri(String path) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return URI.create(normalizedBaseUrl() + normalizedPath);
    }
}
