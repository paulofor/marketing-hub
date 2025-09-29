package com.marketinghub.facebookadsworker;

import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FacebookAccessTokenManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookAccessTokenManager.class);

    private final FacebookAdsService facebookAdsService;
    private final FacebookWorkerConfigurationClient configurationClient;

    public FacebookAccessTokenManager(
        FacebookAdsService facebookAdsService,
        FacebookWorkerConfigurationClient configurationClient
    ) {
        this.facebookAdsService = facebookAdsService;
        this.configurationClient = configurationClient;
    }

    public RenewalAttemptResult tryRenewAccessTokenIfPossible() {
        var configuration = configurationClient.fetchConfiguration();
        if (configuration.isEmpty()) {
            LOGGER.debug("Skipping automatic Facebook token renewal because worker configuration is unavailable");
            return new RenewalAttemptResult(RenewalOutcome.NOT_CONFIGURED, null, null);
        }

        String appId = normalize(configuration.get().appId());
        String appSecret = normalize(configuration.get().appSecret());
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            LOGGER.debug("Skipping automatic Facebook token renewal because app credentials are missing in the worker configuration");
            return new RenewalAttemptResult(RenewalOutcome.NOT_CONFIGURED, null, null);
        }

        String currentToken = facebookAdsService.getCurrentAccessToken();
        if (!StringUtils.hasText(currentToken)) {
            LOGGER.warn("Skipping automatic Facebook token renewal because the current access token is not configured");
            return new RenewalAttemptResult(RenewalOutcome.NOT_CONFIGURED, null, null);
        }

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
