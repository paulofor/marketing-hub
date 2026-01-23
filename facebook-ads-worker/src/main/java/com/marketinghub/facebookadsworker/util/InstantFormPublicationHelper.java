package com.marketinghub.facebookadsworker.util;

import org.slf4j.Logger;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

public final class InstantFormPublicationHelper {
    private InstantFormPublicationHelper() {
    }

    public static String buildInstantFormShareLink(String facebookFormId) {
        if (!StringUtils.hasText(facebookFormId)) {
            return null;
        }
        return "https://www.facebook.com/ads/leadgen/?id=" + facebookFormId;
    }

    public static String normalizeInstantFormId(Logger logger, String facebookFormId, String shareLink) {
        String fromShareLink = extractInstantFormIdFromShareLink(logger, shareLink);
        if (StringUtils.hasText(fromShareLink)) {
            return fromShareLink;
        }
        if (!StringUtils.hasText(facebookFormId)) {
            return null;
        }
        String trimmed = facebookFormId.trim();
        if (trimmed.startsWith("ai_form_")) {
            String normalized = trimmed.replaceFirst("^ai_", "");
            if (logger != null) {
                logger.info(
                    "Normalized placeholder instant form identifier {} to {} while waiting for Meta confirmation",
                    trimmed,
                    normalized
                );
            }
            return normalized;
        }
        return trimmed;
    }

    public static String extractInstantFormIdFromShareLink(Logger logger, String shareLink) {
        if (!StringUtils.hasText(shareLink)) {
            return null;
        }
        try {
            var uriComponents = UriComponentsBuilder.fromUriString(shareLink).build();
            String id = uriComponents.getQueryParams().getFirst("form_id");
            if (!StringUtils.hasText(id)) {
                id = uriComponents.getQueryParams().getFirst("id");
            }
            return StringUtils.hasText(id) ? id.trim() : null;
        } catch (IllegalArgumentException ex) {
            if (logger != null) {
                logger.warn(
                    "Could not extract instant form identifier from share link {}: {}",
                    shareLink,
                    ex.getMessage()
                );
            }
            return null;
        }
    }
}
