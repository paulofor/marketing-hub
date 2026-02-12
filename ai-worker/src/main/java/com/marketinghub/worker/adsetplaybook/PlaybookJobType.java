package com.marketinghub.worker.adsetplaybook;

/**
 * Job types emitted by the backend playbook coordinator.
 */
public enum PlaybookJobType {
    AI_PREPARE_SEED,
    FACEBOOK_SEED_LOOKUP,
    FACEBOOK_TARGETING_SUGGESTIONS,
    FACEBOOK_SOCIAL_POSITIONS,
    AI_BUILD_SPECS,
    FACEBOOK_VALIDATE_SPEC,
    FACEBOOK_REACH_ESTIMATE,
    AI_RECALIBRATE_SPEC
}
