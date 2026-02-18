package com.marketinghub.facebookads.playbook;

/**
 * Categorizes Graph API calls for experiments so the UI can distinguish contexts.
 */
public enum ExperimentFacebookApiLogContext {
    PLAYBOOK,
    CAMPAIGN_CREATION,
    CAMPAIGN_AD_SET,
    CAMPAIGN_AD_CREATIVE,
    CAMPAIGN_AD
}
