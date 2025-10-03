package com.marketinghub.facebookadsworker;

import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient;
import com.marketinghub.facebookadsworker.facebooktokenrenewal.FacebookTokenRevalidationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FacebookAccessTokenManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookAccessTokenManager.class);

    private final FacebookAdsService facebookAdsService;
    private final FacebookWorkerConfigurationClient configurationClient;
    private final FacebookTokenRevalidationClient tokenRevalidationClient;

    public FacebookAccessTokenManager(
        FacebookAdsService facebookAdsService,
        FacebookWorkerConfigurationClient configurationClient,
        FacebookTokenRevalidationClient tokenRevalidationClient
    ) {
        this.facebookAdsService = facebookAdsService;
        this.configurationClient = configurationClient;
        this.tokenRevalidationClient = tokenRevalidationClient;
    }

    public RenewalAttemptResult tryRenewAccessTokenIfPossible() {
        var configuration = configurationClient.fetchConfiguration();
        if (configuration.isEmpty()) {
            LOGGER.debug("Skipping automatic Facebook token renewal because worker configuration is unavailable");
            return new RenewalAttemptResult(RenewalOutcome.NOT_CONFIGURED, null, null);
        }

        Long accountId = configuration.get().accountId();
        if (accountId == null) {
            LOGGER.debug("Skipping automatic Facebook token renewal because the worker configuration is missing the account identifier");
            return new RenewalAttemptResult(RenewalOutcome.NOT_CONFIGURED, null, null);
        }

        String currentToken = facebookAdsService.getCurrentAccessToken();
        if (!StringUtils.hasText(currentToken)) {
            LOGGER.warn("Skipping automatic Facebook token renewal because the current access token is not configured");
            return new RenewalAttemptResult(RenewalOutcome.NOT_CONFIGURED, null, null);
        }

        try {
            FacebookTokenRevalidationClient.TokenRevalidationResponse response = tokenRevalidationClient
                .revalidate(accountId)
                .orElse(null);

            if (response == null) {
                LOGGER.error("Failed to renew Facebook access token automatically: backend returned an empty response");
                return new RenewalAttemptResult(RenewalOutcome.FAILED, null, "Empty response from backend");
            }

            if (response.status() == FacebookTokenRevalidationClient.TokenRevalidationStatus.SUCCESS) {
                if (StringUtils.hasText(response.accessToken())) {
                    facebookAdsService.updateAccessToken(response.accessToken());
                }
                LOGGER.info("Facebook access token renewed successfully via backend token generation");
                return new RenewalAttemptResult(RenewalOutcome.SUCCESS, response.accessToken(), null);
            }

            String errorMessage = StringUtils.hasText(response.errorMessage())
                ? response.errorMessage()
                : "unknown error";
            LOGGER.error("Failed to renew Facebook access token automatically: {}", errorMessage);
            return new RenewalAttemptResult(RenewalOutcome.FAILED, null, errorMessage);
        } catch (Exception ex) {
            LOGGER.error("Failed to renew Facebook access token automatically: {}", ex.getMessage(), ex);
            return new RenewalAttemptResult(RenewalOutcome.FAILED, null, ex.getMessage());
        }
    }

    public enum RenewalOutcome {
        SUCCESS,
        NOT_CONFIGURED,
        FAILED
    }

    public record RenewalAttemptResult(RenewalOutcome outcome, String newToken, String errorMessage) {}
}
