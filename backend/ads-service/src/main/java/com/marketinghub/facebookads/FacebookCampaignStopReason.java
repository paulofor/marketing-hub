package com.marketinghub.facebookads;

/**
 * Motivos que disparam uma solicitação automática de pausa para uma campanha Facebook.
 */
public enum FacebookCampaignStopReason {
    FORM_ZERO_CONVERSION_RULE_OF_THREE,
    LOW_IMPRESSIONS_AFTER_RUNNING_TIME,
    TARGET_AUDIENCE_LOW_INTEREST_STATISTICAL
}
