package com.marketinghub.leadportal.model;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import org.springframework.util.StringUtils;

public record FlowAccessMetadata(String clientIp, String userAgent, String referer, String visitorId) {

    public static final String VISITOR_COOKIE_NAME = "lead_flow_visitor";

    public static FlowAccessMetadata from(HttpServletRequest request) {
        return new FlowAccessMetadata(resolveClientIp(request),
                trimToNull(request.getHeader("User-Agent")),
                trimToNull(request.getHeader("Referer")),
                extractVisitorId(request));
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String extractVisitorId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> VISITOR_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .findFirst()
                .orElse(null);
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
