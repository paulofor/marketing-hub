package com.marketinghub.watermark.service;

import com.marketinghub.watermark.config.WatermarkProperties;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class WatermarkKeyResolver {

    private final WatermarkProperties properties;

    public WatermarkKeyResolver(WatermarkProperties properties) {
        this.properties = properties;
    }

    public String buildKey(long packageId, long itemId, String extension) {
        String prefix = properties.getOutputPrefix();
        if (prefix == null || prefix.isBlank()) {
            prefix = "watermarks";
        }
        String normalizedPrefix = prefix.replaceAll("^/+", "").replaceAll("/+\\z", "");
        if (!normalizedPrefix.isEmpty()) {
            normalizedPrefix = normalizedPrefix + "/";
        }
        String safeExtension = (extension == null || extension.isBlank()) ? "png" : extension.toLowerCase();
        String uniqueToken = UUID.randomUUID().toString().replace("-", "");
        return "%slead-portal/package-%d/item-%d-%s.%s".formatted(
                normalizedPrefix, packageId, itemId, uniqueToken, safeExtension);
    }
}
