package com.marketinghub.emailservice.service;

import com.marketinghub.emailservice.config.EmailTrackingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class TrackingPixelService {

    private static final Logger log = LoggerFactory.getLogger(TrackingPixelService.class);

    private final EmailTrackingProperties emailTrackingProperties;

    public TrackingPixelService(EmailTrackingProperties emailTrackingProperties) {
        this.emailTrackingProperties = emailTrackingProperties;
    }

    public String buildTrackingPixelUrl(String requestId) {
        if (!StringUtils.hasText(emailTrackingProperties.baseUrl()) || !StringUtils.hasText(requestId)) {
            return null;
        }

        try {
            return UriComponentsBuilder.fromUriString(emailTrackingProperties.baseUrl())
                    .pathSegment(requestId + ".png")
                    .build()
                    .toUriString();
        } catch (IllegalArgumentException ex) {
            log.warn("Tracking base URL inválida: {}", emailTrackingProperties.baseUrl(), ex);
            return null;
        }
    }

    public String appendTrackingPixel(String htmlBody, String trackingPixelUrl) {
        if (!StringUtils.hasText(htmlBody) || !StringUtils.hasText(trackingPixelUrl)) {
            return htmlBody;
        }

        String trackingImageTag = "<img src=\"" + trackingPixelUrl
                + "\" alt=\"\" style=\"display:none;width:1px;height:1px;\" width=\"1\" height=\"1\" />";
        return htmlBody + trackingImageTag;
    }
}
