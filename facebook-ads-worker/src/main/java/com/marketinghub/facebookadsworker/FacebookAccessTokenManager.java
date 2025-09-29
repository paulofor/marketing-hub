package com.marketinghub.facebookadsworker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FacebookAccessTokenManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookAccessTokenManager.class);

    private final FacebookAdsService facebookAdsService;
    private final String appId;
    private final String appSecret;

    public FacebookAccessTokenManager(
        FacebookAdsService facebookAdsService,
        @Value("${facebook.app-id:}") String appId,
        @Value("${facebook.app-secret:}") String appSecret
    ) {
        this.facebookAdsService = facebookAdsService;
        this.appId = normalize(appId);
        this.appSecret = normalize(appSecret);
    }

    public RenewalAttemptResult tryRenewAccessTokenIfPossible() {
        if (!isConfigured()) {
            LOGGER.debug("Skipping automatic Facebook token renewal because app credentials are missing");
            return new RenewalAttemptResult(RenewalOutcome.NOT_CONFIGURED, null, null);
        }

        String currentToken = facebookAdsService.getCurrentAccessToken();
        try {
            FacebookAdsService.TokenRenewalResponse response = facebookAdsService.renewLongLivedToken(
                appId,
                appSecret,
                currentToken
            );
            facebookAdsService.updateAccessToken(response.accessToken());
            LOGGER.info(
                "Facebook access token renewed successfully via Graph API; expiresInSeconds={}",
                response.expiresInSeconds()
            );
            return new RenewalAttemptResult(RenewalOutcome.SUCCESS, response.accessToken(), null);
        } catch (Exception ex) {
            LOGGER.error("Failed to renew Facebook access token automatically: {}", ex.getMessage(), ex);
            return new RenewalAttemptResult(RenewalOutcome.FAILED, null, ex.getMessage());
        }
    }

    private boolean isConfigured() {
        return StringUtils.hasText(appId) && StringUtils.hasText(appSecret);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    public enum RenewalOutcome {
        SUCCESS,
        NOT_CONFIGURED,
        FAILED
    }

    public record RenewalAttemptResult(RenewalOutcome outcome, String newToken, String errorMessage) {}
}
