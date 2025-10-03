package com.marketinghub.facebookadsworker;

import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient;
import com.marketinghub.facebookadsworker.facebooktokenrenewal.FacebookTokenRenewalClient.FacebookTokenRenewalStatus;
import com.marketinghub.facebookadsworker.facebooktokenrenewal.FacebookTokenRenewalService;
import com.marketinghub.facebookadsworker.facebooktokenrenewal.FacebookTokenRenewalService.TokenRenewalAttemptResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FacebookAccessTokenManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookAccessTokenManager.class);

    private final FacebookAdsService facebookAdsService;
    private final FacebookWorkerConfigurationClient configurationClient;
    private final FacebookTokenRenewalService tokenRenewalService;

    public FacebookAccessTokenManager(
        FacebookAdsService facebookAdsService,
        FacebookWorkerConfigurationClient configurationClient,
        FacebookTokenRenewalService tokenRenewalService
    ) {
        this.facebookAdsService = facebookAdsService;
        this.configurationClient = configurationClient;
        this.tokenRenewalService = tokenRenewalService;
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

        if (!StringUtils.hasText(configuration.get().appId()) || !StringUtils.hasText(configuration.get().appSecret())) {
            LOGGER.warn(
                "Skipping automatic Facebook token renewal because the worker configuration is missing App ID or App Secret"
            );
            return new RenewalAttemptResult(RenewalOutcome.NOT_CONFIGURED, null, "Missing App credentials");
        }

        try {
            TokenRenewalAttemptResult attempt = tokenRenewalService.renewTokenForAccount(
                accountId,
                configuration.get().appId(),
                configuration.get().appSecret(),
                currentToken,
                true
            );

            if (attempt.status() == FacebookTokenRenewalStatus.SUCCESS) {
                LOGGER.info("Facebook access token renewed successfully via Graph API");
                return new RenewalAttemptResult(RenewalOutcome.SUCCESS, attempt.accessToken(), null);
            }

            String errorMessage = StringUtils.hasText(attempt.errorMessage())
                ? attempt.errorMessage()
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
