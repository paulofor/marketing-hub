package com.marketinghub.facebookads;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Simplified Facebook Ads API client.
 *
 * <p>Implementations must read the access token from configuration but
 * should never log its full value for security reasons.</p>
 */
public interface FacebookAdsClient {
    /**
     * Lists ad accounts associated with the current token owner.
     */
    JsonNode getAdAccounts();
}
