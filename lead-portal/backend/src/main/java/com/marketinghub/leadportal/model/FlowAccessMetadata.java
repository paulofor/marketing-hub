package com.marketinghub.leadportal.model;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import org.springframework.util.StringUtils;

public record FlowAccessMetadata(String clientIp, String userAgent, String referer, String visitorId, String campaignCode) {

    private static final String VISITOR_COOKIE_NAME = "marketinghub_visitor_id";
    private static final int MAX_CAMPAIGN_LENGTH = 190;

    public static FlowAccessMetadata from(HttpServletRequest request) {
        return new FlowAccessMetadata(resolveClientIp(request),
                trimToNull(request.getHeader("User-Agent")),
                trimToNull(request.getHeader("Referer")),
                resolveVisitorId(request),
                resolveCampaignCode(request));
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String resolveCampaignCode(HttpServletRequest request) {
        String campaign = trimToNull(request.getParameter("campaign"));
        if (campaign == null) {
            campaign = trimToNull(request.getParameter("utm_campaign"));
        }
        if (campaign == null) {
            return null;
        }
        return campaign.length() > MAX_CAMPAIGN_LENGTH ? campaign.substring(0, MAX_CAMPAIGN_LENGTH) : campaign;
    }

    private static String resolveVisitorId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> VISITOR_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .map(FlowAccessMetadata::trimToNull)
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
