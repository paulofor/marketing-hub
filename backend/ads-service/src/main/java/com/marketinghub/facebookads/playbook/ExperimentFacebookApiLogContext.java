package com.marketinghub.facebookads.playbook;

/**
 * Categoriza chamadas da Graph API por experimento para a UI distinguir os contextos.
 */
public enum ExperimentFacebookApiLogContext {
    PLAYBOOK,
    CAMPAIGN_REACH_VALIDATION,
    CAMPAIGN_CREATION,
    CAMPAIGN_AD_SET,
    CAMPAIGN_AD_CREATIVE,
    CAMPAIGN_AD,
    TARGETING_SIMPLE_FLOW
}
