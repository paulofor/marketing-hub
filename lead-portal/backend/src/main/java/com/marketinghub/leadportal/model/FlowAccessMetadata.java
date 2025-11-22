package com.marketinghub.leadportal.model;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public record FlowAccessMetadata(String clientIp, String userAgent, String referer) {

    public static FlowAccessMetadata from(HttpServletRequest request) {
        return new FlowAccessMetadata(resolveClientIp(request),
                trimToNull(request.getHeader("User-Agent")),
                trimToNull(request.getHeader("Referer")));
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
