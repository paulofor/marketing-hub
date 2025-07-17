package com.marketinghub.facebookads;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Cliente simplificado para a API do Facebook Ads.
 */
public interface FacebookAdsClient {
    /**
     * Lista as contas de anúncio associadas ao usuário.
     */
    JsonNode getAdAccounts();
}
